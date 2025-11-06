/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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


import com.sun.javafx.geometry.BoundsUtils;
import com.sun.javafx.scene.traversal.TraversalMethod;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Window;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.glass.ui.Accessible;
import com.sun.glass.ui.Application;
import com.sun.javafx.util.Logging;
import com.sun.javafx.util.TempState;
import com.sun.javafx.util.Utils;
import com.sun.javafx.beans.IDProperty;
import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.collections.UnmodifiableListSet;
import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionDefinitionConverter;
import com.sun.javafx.css.TransitionDefinitionCssMetaData;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.css.media.MediaQueryContext;
import javafx.css.Selector;
import javafx.css.Style;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.CursorConverter;
import javafx.css.converter.EffectConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.scene.AbstractNode;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.scene.CameraHelper;
import com.sun.javafx.scene.CssFlags;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.EventHandlerProperties;
import com.sun.javafx.scene.LayoutFlags;
import com.sun.javafx.scene.NodeEventDispatcher;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.scene.SceneUtils;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.transform.TransformHelper;
import com.sun.javafx.scene.transform.TransformUtils;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.impl.PrismSettings;
import com.sun.scenario.effect.EffectHelper;

import javafx.scene.shape.Shape3D;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

/**
 * Base class for scene graph nodes. A scene graph is a set of tree data structures
 * where every item has zero or one parent, and each item is either
 * a "leaf" with zero sub-items or a "branch" with zero or more sub-items.
 * <p>
 * Each item in the scene graph is called a {@code Node}. Branch nodes are
 * of type {@link Parent}, whose concrete subclasses are {@link Group},
 * {@link javafx.scene.layout.Region}, and {@link javafx.scene.control.Control},
 * or subclasses thereof.
 * <p>
 * Leaf nodes are classes such as
 * {@link javafx.scene.shape.Rectangle}, {@link javafx.scene.text.Text},
 * {@link javafx.scene.image.ImageView}, {@link javafx.scene.media.MediaView},
 * or other such leaf classes which cannot have children. Only a single node within
 * each scene graph tree will have no parent, which is referred to as the "root" node.
 * <p>
 * There may be several trees in the scene graph. Some trees may be part of
 * a {@link Scene}, in which case they are eligible to be displayed.
 * Other trees might not be part of any {@link Scene}.
 * <p>
 * A node may occur at most once anywhere in the scene graph. Specifically,
 * a node must appear no more than once in all of the following:
 * as the root node of a {@link Scene},
 * the children ObservableList of a {@link Parent},
 * or as the clip of a {@link Node}.
 * <p>
 * The scene graph must not have cycles. A cycle would exist if a node is
 * an ancestor of itself in the tree, considering the {@link Group} content
 * ObservableList, {@link Parent} children ObservableList, and {@link Node} clip relationships
 * mentioned above.
 * <p>
 * If a program adds a child node to a Parent (including Group, Region, etc)
 * and that node is already a child of a different Parent or the root of a Scene,
 * the node is automatically (and silently) removed from its former parent.
 * If a program attempts to modify the scene graph in any other way that violates
 * the above rules, an exception is thrown, the modification attempt is ignored
 * and the scene graph is restored to its previous state.
 * <p>
 * It is possible to rearrange the structure of the scene graph, for
 * example, to move a subtree from one location in the scene graph to
 * another. In order to do this, one would normally remove the subtree from
 * its old location before inserting it at the new location. However, the
 * subtree will be automatically removed as described above if the application
 * doesn't explicitly remove it.
 * <p>
 * Node objects may be constructed and modified on any thread as long they are
 * not yet attached to a {@link Scene} in a {@link Window} that is
 * {@link Window#isShowing showing}.
 * An application must attach nodes to such a Scene or modify them on the JavaFX
 * Application Thread.
 *
 * <p>
 * The JavaFX Application Thread is created as part of the startup process for
 * the JavaFX runtime. See the {@link javafx.application.Application} class and
 * the {@link Platform#startup(Runnable)} method for more information.
 * </p>
 *
 * <h2><a id="StringID">String ID</a></h2>
 * <p>
 * Each node in the scene graph can be given a unique {@link #idProperty id}. This id is
 * much like the "id" attribute of an HTML tag in that it is up to the designer
 * and developer to ensure that the {@code id} is unique within the scene graph.
 * A convenience function called {@link #lookup(String)} can be used to find
 * a node with a unique id within the scene graph, or within a subtree of the
 * scene graph. The id can also be used identify nodes for applying styles; see
 * the CSS section below.
 *
 * <h2><a id="CoordinateSystem">Coordinate System</a></h2>
 * <p>
 * The {@code Node} class defines a traditional computer graphics "local"
 * coordinate system in which the {@code x} axis increases to the right and the
 * {@code y} axis increases downwards.  The concrete node classes for shapes
 * provide variables for defining the geometry and location of the shape
 * within this local coordinate space.  For example,
 * {@link javafx.scene.shape.Rectangle} provides {@code x}, {@code y},
 * {@code width}, {@code height} variables while
 * {@link javafx.scene.shape.Circle} provides {@code centerX}, {@code centerY},
 * and {@code radius}.
 * <p>
 * At the device pixel level, integer coordinates map onto the corners and
 * cracks between the pixels and the centers of the pixels appear at the
 * midpoints between integer pixel locations.  Because all coordinate values
 * are specified with floating point numbers, coordinates can precisely
 * point to these corners (when the floating point values have exact integer
 * values) or to any location on the pixel.  For example, a coordinate of
 * {@code (0.5, 0.5)} would point to the center of the upper left pixel on the
 * {@code Stage}.  Similarly, a rectangle at {@code (0, 0)} with dimensions
 * of {@code 10} by {@code 10} would span from the upper left corner of the
 * upper left pixel on the {@code Stage} to the lower right corner of the
 * 10th pixel on the 10th scanline.  The pixel center of the last pixel
 * inside that rectangle would be at the coordinates {@code (9.5, 9.5)}.
 * <p>
 * In practice, most nodes have transformations applied to their coordinate
 * system as mentioned below.  As a result, the information above describing
 * the alignment of device coordinates to the pixel grid is relative to
 * the transformed coordinates, not the local coordinates of the nodes.
 * The {@link javafx.scene.shape.Shape Shape} class describes some additional
 * important context-specific information about coordinate mapping and how
 * it can affect rendering.
 *
 * <h2><a id="Transformations">Transformations</a></h2>
 * <p>
 * Any {@code Node} can have transformations applied to it. These include
 * translation, rotation, scaling, or shearing.
 * <p>
 * A <b>translation</b> transformation is one which shifts the origin of the
 * node's coordinate space along either the x or y axis. For example, if you
 * create a {@link javafx.scene.shape.Rectangle} which is drawn at the origin
 * (x=0, y=0) and has a width of 100 and a height of 50, and then apply a
 * {@link javafx.scene.transform.Translate} with a shift of 10 along the x axis
 * (x=10), then the rectangle will appear drawn at (x=10, y=0) and remain
 * 100 points wide and 50 tall. Note that the origin was shifted, not the
 * {@code x} variable of the rectangle.
 * <p>
 * A common node transform is a translation by an integer distance, most often
 * used to lay out nodes on the stage.  Such integer translations maintain the
 * device pixel mapping so that local coordinates that are integers still
 * map to the cracks between pixels.
 * <p>
 * A <b>rotation</b> transformation is one which rotates the coordinate space of
 * the node about a specified "pivot" point, causing the node to appear rotated.
 * For example, if you create a {@link javafx.scene.shape.Rectangle} which is
 * drawn at the origin (x=0, y=0) and has a width of 100 and height of 30 and
 * you apply a {@link javafx.scene.transform.Rotate} with a 90 degree rotation
 * (angle=90) and a pivot at the origin (pivotX=0, pivotY=0), then
 * the rectangle will be drawn as if its x and y were zero but its height was
 * 100 and its width -30. That is, it is as if a pin is being stuck at the top
 * left corner and the rectangle is rotating 90 degrees clockwise around that
 * pin. If the pivot point is instead placed in the center of the rectangle
 * (at point x=50, y=15) then the rectangle will instead appear to rotate about
 * its center.
 * <p>
 * Note that as with all transformations, the x, y, width, and height variables
 * of the rectangle (which remain relative to the local coordinate space) have
 * not changed, but rather the transformation alters the entire coordinate space
 * of the rectangle.
 * <p>
 * A <b>scaling</b> transformation causes a node to either appear larger or
 * smaller depending on the scaling factor. Scaling alters the coordinate space
 * of the node such that each unit of distance along the axis in local
 * coordinates is multiplied by the scale factor. As with rotation
 * transformations, scaling transformations are applied about a "pivot" point.
 * You can think of this as the point in the {@code Node} around which you "zoom".  For
 * example, if you create a {@link javafx.scene.shape.Rectangle} with a
 * {@code strokeWidth} of 5, and a width and height of 50, and you apply a
 * {@link javafx.scene.transform.Scale} with scale factors (x=2.0, y=2.0) and
 * a pivot at the origin (pivotX=0, pivotY=0), the entire rectangle
 * (including the stroke) will double in size, growing to the right and
 * downwards from the origin.
 * <p>
 * A <b>shearing</b> transformation, sometimes called a skew, effectively
 * rotates one axis so that the x and y axes are no longer perpendicular.
 * <p>
 * Multiple transformations may be applied to a node. Custom transforms are applied using the
 * {@link #getTransforms transforms} list. Predefined transforms are applied using the properties specified below.
 * The matrices that represent the transforms are multiplied in this order:
 * <ol>
 * <li> Layout ({@link #layoutXProperty layoutX}, {@link #layoutYProperty layoutY}) and translate
 * ({@link #translateXProperty translateX}, {@link #translateYProperty translateY}, {@link #translateZProperty translateZ})</li>
 * <li> Rotate ({@link #rotateProperty rotate})</li>
 * <li> Scale ({@link #scaleXProperty scaleX}, {@link #scaleYProperty scaleY}, {@link #scaleZProperty scaleZ})</li>
 * <li> Transforms list ({@link #getTransforms transforms}) starting from element 0</li>
 * </ol>
 * The transforms are applied in the reverse order of the matrix multiplication outlined above: last element of the transforms list
 * to 0th element, scale, rotate, and layout and translate. By applying the transforms in this order, the bounds in the local
 * coordinates of the node are transformed to the bounds in the parent coordinate of the node (see the
 * <a href="#BoundingRectangles">Bounding Rectangles</a> section).
 *
 * <h2><a id="BoundingRectangles">Bounding Rectangles</a></h2>
 * <p>
 * Since every {@code Node} has transformations, every Node's geometric
 * bounding rectangle can be described differently depending on whether
 * transformations are accounted for or not.
 * <p>
 * Each {@code Node} has a read-only {@link #boundsInLocalProperty boundsInLocal}
 * variable which specifies the bounding rectangle of the {@code Node} in
 * untransformed local coordinates. {@code boundsInLocal} includes the
 * Node's shape geometry, including any space required for a
 * non-zero stroke that may fall outside the local position/size variables,
 * and its {@link #clipProperty clip} and {@link #effectProperty effect} variables.
 * <p>
 * Each {@code Node} also has a read-only {@link #boundsInParentProperty boundsInParent} variable which
 * specifies the bounding rectangle of the {@code Node} after all transformations
 * have been applied as specified in the <a href="#Transformations">Transformations</a> section.
 * It is called "boundsInParent" because the rectangle will be relative to the
 * parent's coordinate system.  This is the 'visual' bounds of the node.
 * <p>
 * Finally, the {@link #layoutBoundsProperty layoutBounds} variable defines the rectangular bounds of
 * the {@code Node} that should be used as the basis for layout calculations and
 * may differ from the visual bounds of the node.  For shapes, Text, and ImageView,
 * layoutBounds by default includes only the shape geometry, including space required
 * for a non-zero {@code strokeWidth}, but does <i>not</i> include the effect,
 * clip, or any transforms. For resizable classes (Regions and Controls)
 * layoutBounds will always map to {@code 0,0 width x height}.
 *
 * <p> The image shows a node without any transformation and its {@code boundsInLocal}:
 * <p> <img src="doc-files/boundsLocal.png" alt="A sine wave shape enclosed by
 * an axis-aligned rectangular bounds"> </p>
 * If we rotate the image by 20 degrees we get following result:
 * <p> <img src="doc-files/boundsParent.png" alt="An axis-aligned rectangular
 * bounds that encloses the shape rotated by 20 degrees"> </p>
 * The red rectangle represents {@code boundsInParent} in the
 * coordinate space of the Node's parent. The {@code boundsInLocal} stays the same
 * as in the first image, the green rectangle in this image represents {@code boundsInLocal}
 * in the coordinate space of the Node.
 *
 * <p> The images show a filled and stroked rectangle and their bounds. The
 * first rectangle {@code [x:10.0 y:10.0 width:100.0 height:100.0 strokeWidth:0]}
 * has the following bounds bounds: {@code [x:10.0 y:10.0 width:100.0 height:100.0]}.
 *
 * The second rectangle {@code [x:10.0 y:10.0 width:100.0 height:100.0 strokeWidth:5]}
 * has the following bounds: {@code [x:7.5 y:7.5 width:105 height:105]}
 * (the stroke is centered by default, so only half of it is outside
 * of the original bounds; it is also possible to create inside or outside
 * stroke).
 *
 * Since neither of the rectangles has any transformation applied,
 * {@code boundsInParent} and {@code boundsInLocal} are the same. </p>
 * <p> <img src="doc-files/bounds.png" alt="The rectangles are enclosed by their
 * respective bounds"> </p>
 *
 * <h2><a id="CSS">CSS</a></h2>
 * <p>
 * The {@code Node} class contains {@code id}, {@code styleClass}, and
 * {@code style} variables that are used in styling this node from
 * CSS. The {@code id} and {@code styleClass} variables are used in
 * CSS style sheets to identify nodes to which styles should be
 * applied. The {@code style} variable contains style properties and
 * values that are applied directly to this node.
 * <p>
 * For further information about CSS and how to apply CSS styles
 * to nodes, see the <a href="doc-files/cssref.html">CSS Reference
 * Guide</a>.
 *
 * @since JavaFX 2.0
 */
@IDProperty("id")
public abstract sealed class Node
        implements EventTarget, Styleable
        permits AbstractNode, Camera, LightBase, Parent, SubScene, Canvas, ImageView, Shape, Shape3D {

    /*
     * Store the singleton instance of the NodeHelper subclass corresponding
     * to the subclass of this instance of Node
     */
    private NodeHelper nodeHelper = null;

    static {
        PerformanceTracker.logEvent("Node class loaded");

        // This is used by classes in different packages to get access to
        // private and package private methods.
        NodeHelper.setNodeAccessor(new NodeHelper.NodeAccessor() {
            @Override
            public NodeHelper getHelper(Node node) {
                return node.nodeHelper;
            }

            @Override
            public void setHelper(Node node, NodeHelper nodeHelper) {
                node.nodeHelper = nodeHelper;
            }

            @Override
            public void doMarkDirty(Node node, DirtyBits dirtyBit) {
                node.doMarkDirty(dirtyBit);
            }

            @Override
            public void doUpdatePeer(Node node) {
                node.doUpdatePeer();
            }

            @Override
            public BaseTransform getLeafTransform(Node node) {
                return node.getLeafTransform();
            }

            @Override
            public Bounds doComputeLayoutBounds(Node node) {
                return node.doComputeLayoutBounds();
            }

            @Override
            public void doTransformsChanged(Node node) {
                node.doTransformsChanged();
            }

            @Override
            public void doPickNodeLocal(Node node, PickRay localPickRay,
                    PickResultChooser result) {
                node.doPickNodeLocal(localPickRay, result);
            }

            @Override
            public boolean doComputeIntersects(Node node, PickRay pickRay,
                    PickResultChooser pickResult) {
                return node.doComputeIntersects(pickRay, pickResult);
            }

            @Override
            public void doGeomChanged(Node node) {
                node.doGeomChanged();
            }

            @Override
            public void doNotifyLayoutBoundsChanged(Node node) {
                node.doNotifyLayoutBoundsChanged();
            }

            @Override
            public void doProcessCSS(Node node) {
                node.doProcessCSS();
            }

            @Override
            public boolean isDirty(Node node, DirtyBits dirtyBit) {
                return node.isDirty(dirtyBit);
            }

            @Override
            public boolean isDirtyEmpty(Node node) {
                return node.isDirtyEmpty();
            }

            @Override
            public void setScenes(Node node, Scene newScene, SubScene newSubScene) {
                node.setScenes(newScene, newSubScene);
            }

            @Override
            public void setParent(Node node, Parent parent) {
                node.setParent(parent);
            }

            @Override
            public void updateBounds(Node node) {
                node.updateBounds();
            }

            @Override
            public void syncPeer(Node node) {
                node.syncPeer();
            }

            @Override
            public void layoutBoundsChanged(Node node) {
                node.layoutBoundsChanged();
            }

            @Override
            public void nodeResolvedOrientationInvalidated(Node node) {
                node.nodeResolvedOrientationInvalidated();
            }

            @Override
            public void setInheritOrientationFromScene(Node node, boolean value) {
                node.setInheritOrientationFromScene(value);
            }

            @Override
            public <P extends NGNode> P getPeer(Node node) {
                return node.getPeer();
            }

            @Override
            public void setShowMnemonics(Node node, boolean value) {
                node.setShowMnemonics(value);
            }

            @Override
            public boolean isShowMnemonics(Node node) {
                return node.isShowMnemonics();
            }

            @Override
            public BooleanProperty showMnemonicsProperty(Node node) {
                return node.showMnemonicsProperty();
            }

            @Override
            public boolean traverse(Node node, Direction direction, TraversalMethod method) {
                return node.traverse(direction, method);
            }

            @Override
            public double getPivotX(Node node) {
                return node.getPivotX();
            }

            @Override
            public double getPivotY(Node node) {
                return node.getPivotY();
            }

            @Override
            public double getPivotZ(Node node) {
                return node.getPivotZ();
            }

            @Override
            public void pickNode(Node node,PickRay pickRay,
                    PickResultChooser result) {
                node.pickNode(pickRay, result);
            }

            @Override
            public boolean intersects(Node node, PickRay pickRay,
                    PickResultChooser pickResult) {
                return node.intersects(pickRay, pickResult);
            }

            @Override
            public double intersectsBounds(Node node, PickRay pickRay) {
                return node.intersectsBounds(pickRay);
            }

            @Override
            public void layoutNodeForPrinting(Node node) {
                node.doCSSLayoutSyncForSnapshot();
            }

            @Override
            public boolean isDerivedDepthTest(Node node) {
                return node.isDerivedDepthTest();
            }

            @Override
            public SubScene getSubScene(Node node) {
                return node.getSubScene();
            }

            @Override
            public void setLabeledBy(Node node, Node labeledBy) {
                node.labeledBy = labeledBy;
            }

            @Override
            public Accessible getAccessible(Node node) {
                return node.getAccessible();
            }

            @Override
            public void reapplyCSS(Node node) {
                node.reapplyCSS();
            }

            @Override
            public boolean isInitialCssState(Node node) {
                return node.initialCssState;
            }

            @Override
            public void recalculateRelativeSizeProperties(Node node, Font fontForRelativeSizes) {
                node.recalculateRelativeSizeProperties(fontForRelativeSizes);
            }

            @Override
            public boolean isTreeVisible(Node node) {
                return node.isTreeVisible();
            }

            @Override
            public BooleanExpression treeVisibleProperty(Node node) {
                return node.treeVisibleProperty();
            }

            @Override
            public boolean isTreeShowing(Node node) {
                return node.isTreeShowing();
            }

            @Override
            public List<Style> getMatchingStyles(CssMetaData cssMetaData,
                    Styleable styleable) {
                return Node.getMatchingStyles(cssMetaData, styleable);
            }

            @Override
            public Map<StyleableProperty<?>, List<Style>> findStyles(Node node,
                    Map<StyleableProperty<?>, List<Style>> styleMap) {
                return node.findStyles(styleMap);
            }

            @Override
            public void requestFocusVisible(Node node) {
                node.requestFocusVisible();
            }

            @Override
            public StyleableProperty<TransitionDefinition[]> getTransitionProperty(Node node) {
                var definitions = node.miscProperties != null ? node.miscProperties.transitionDefinitions : null;
                if (definitions == null) {
                    definitions = new TransitionDefinitionCollection();
                    node.getMiscProperties().transitionDefinitions = definitions;
                }

                return definitions;
            }

            @Override
            public TransitionDefinition findTransitionDefinition(
                    Node node, CssMetaData<? extends Styleable, ?> metadata) {
                var definitions = node.miscProperties != null ? node.miscProperties.transitionDefinitions : null;
                return definitions == null ? null : definitions.find(metadata);
            }

            @Override
            public Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> findTransitionDefinitions(
                    Node node, CssMetaData<? extends Styleable, ?> metadata) {
                var definitions = node.miscProperties != null ? node.miscProperties.transitionDefinitions : null;
                return definitions == null ? null : definitions.findAll(metadata);
            }

            @Override
            public void addTransitionTimer(Node node, String propertyName, TransitionTimer timer) {
                node.addTransitionTimer(propertyName, timer);
            }

            @Override
            public void removeTransitionTimer(Node node, String propertyName) {
                node.removeTransitionTimer(propertyName);
            }

            @Override
            public TransitionTimer findTransitionTimer(Node node, String propertyName) {
                return node.findTransitionTimer(propertyName);
            }

            @Override
            public MediaQueryContext getMediaQueryContext(Node node) {
                return node.getMediaQueryContext();
            }
        });
    }

    /* ************************************************************************
     *                                                                        *
     * Methods and state for managing the dirty bits of a Node. The dirty     *
     * bits are flags used to keep track of what things are dirty on the      *
     * node and therefore need processing on the next pulse. Since the pulse  *
     * happens asynchronously to the change that made the node dirty (for     *
     * performance reasons), we need to keep track of what things have        *
     * changed.                                                               *
     *                                                                        *
     *************************************************************************/

    /**
     * Set of dirty bits that are set when state is invalidated and cleared by
     * the updateState method, which is called from the synchronizer.
     * <p>
     * A node starts dirty.
     */
    private Set<DirtyBits> dirtyBits = EnumSet.allOf(DirtyBits.class);

    /**
     * Mark the specified bit as dirty, and add this node to the scene's dirty list.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doMarkDirty(DirtyBits dirtyBit) {
        if (isDirtyEmpty()) {
            addToSceneDirtyList();
        }

        dirtyBits.add(dirtyBit);
    }

    private void addToSceneDirtyList() {
        Scene s = getScene();
        if (s != null) {
            s.addToDirtyList(this);
            if (getSubScene() != null) {
                getSubScene().setDirty(this);
            }
        }
    }

    /**
     * Test whether the specified dirty bit is set
     */
    final boolean isDirty(DirtyBits dirtyBit) {
        return dirtyBits.contains(dirtyBit);
    }

    /**
     * Clear the specified dirty bit
     */
    final void clearDirty(DirtyBits dirtyBit) {
        dirtyBits.remove(dirtyBit);
    }

    /**
     * Clear all dirty bits
     */
    private void clearDirty() {
        dirtyBits.clear();
    }

    /**
     * Test whether the set of dirty bits is empty
     */
    private boolean isDirtyEmpty() {
        return dirtyBits.isEmpty();
    }

    /* ************************************************************************
     *                                                                        *
     * Methods for synchronizing state from this Node to its PG peer. This    *
     * should only *ever* be called during synchronization initialized as a   *
     * result of a pulse. Any attempt to synchronize at any other time may    *
     * cause rendering artifacts.                                             *
     *                                                                        *
     *************************************************************************/

    /**
     * Called by the synchronizer to update the state and
     * clear dirtybits of this node in the PG graph
     */
    final void syncPeer() {
        // Do not synchronize invisible nodes unless their visibility has changed
        // or they have requested a forced synchronization
        if (!isDirtyEmpty() && (treeVisible
                                     || isDirty(DirtyBits.NODE_VISIBLE)
                                     || isDirty(DirtyBits.NODE_FORCE_SYNC)))
        {
            NodeHelper.updatePeer(this);
            clearDirty();
        }
    }

    /**
     * A temporary rect used for computing bounds by the various bounds
     * variables. This bounds starts life as a RectBounds, but may be promoted
     * to a BoxBounds if there is a 3D transform mixed into its computation.
     * These two fields were held in a thread local, but were then pulled
     * out of it so that we could compute bounds before holding the
     * synchronization lock. These objects have to be per-instance so
     * that we can pass the right data down to the PG side later during
     * synchronization (rather than statics as they were before).
     */
    private BaseBounds _geomBounds = new RectBounds(0, 0, -1, -1);
    private BaseBounds _txBounds = new RectBounds(0, 0, -1, -1);

    private boolean pendingUpdateBounds = false;

    // Happens before we hold the sync lock
    void updateBounds() {
        // Note: the clip must be handled before the visibility is checked. This is because the visiblity might be
        // changing in the clip and it is going to be synchronized, so it needs to recompute the bounds.
        Node n = getClip();
        if (n != null) {
            n.updateBounds();
        }

        // See syncPeer()
        if (!treeVisible && !isDirty(DirtyBits.NODE_VISIBLE)) {

            // Need to save the dirty bits since they will be cleared even for the
            // case of short circuiting dirty bit processing.
            if (isDirty(DirtyBits.NODE_TRANSFORM)
                    || isDirty(DirtyBits.NODE_TRANSFORMED_BOUNDS)
                    || isDirty(DirtyBits.NODE_BOUNDS)) {
                pendingUpdateBounds = true;
            }

            return;
        }

        // Set transform and bounds dirty bits when this node becomes visible
        if (pendingUpdateBounds) {
            NodeHelper.markDirty(this, DirtyBits.NODE_TRANSFORM);
            NodeHelper.markDirty(this, DirtyBits.NODE_TRANSFORMED_BOUNDS);
            NodeHelper.markDirty(this, DirtyBits.NODE_BOUNDS);

            pendingUpdateBounds = false;
        }

        if (isDirty(DirtyBits.NODE_TRANSFORM) || isDirty(DirtyBits.NODE_TRANSFORMED_BOUNDS)) {
            if (isDirty(DirtyBits.NODE_TRANSFORM)) {
                updateLocalToParentTransform();
            }
            _txBounds = getTransformedBounds(_txBounds,
                                             BaseTransform.IDENTITY_TRANSFORM);
        }

        if (isDirty(DirtyBits.NODE_BOUNDS)) {
            _geomBounds = getGeomBounds(_geomBounds,
                    BaseTransform.IDENTITY_TRANSFORM);
        }

    }

    /*
     * This function is called during synchronization to update the state of the
     * NG Node from the FX Node. Subclasses of Node should override this method
     * and must call NodeHelper.updatePeer(this)
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        final NGNode peer = getPeer();

        // For debug / diagnostic purposes, we will copy across a name for this node down to
        // the NG layer, where we can use the name to figure out what the NGNode represents.
        // An alternative would be to have a back-reference from the NGNode back to the Node it
        // is a peer to, however it was felt that this would make it too easy to communicate back
        // to the Node and possibly violate thread invariants. But of course, we only need to do this
        // if we're going to print the render graph (otherwise all the work we'd do to keep the name
        // properly updated would be a waste).
        if (PrismSettings.printRenderGraph && isDirty(DirtyBits.DEBUG)) {
            final String id = getId();
            String className = getClass().getSimpleName();
            if (className.isEmpty()) {
                className = getClass().getName();
            }
            peer.setName(id == null ? className : id + "(" + className + ")");
        }

        if (isDirty(DirtyBits.NODE_TRANSFORM)) {
            peer.setTransformMatrix(localToParentTx);
        }

        if (isDirty(DirtyBits.NODE_VIEW_ORDER)) {
            peer.setViewOrder(getViewOrder());
        }

        if (isDirty(DirtyBits.NODE_BOUNDS)) {
            peer.setContentBounds(_geomBounds);
        }

        if (isDirty(DirtyBits.NODE_TRANSFORMED_BOUNDS)) {
            peer.setTransformedBounds(_txBounds, !isDirty(DirtyBits.NODE_BOUNDS));
        }

        if (isDirty(DirtyBits.NODE_OPACITY)) {
            peer.setOpacity((float)Utils.clamp(0, getOpacity(), 1));
        }

        if (isDirty(DirtyBits.NODE_CACHE)) {
            peer.setCachedAsBitmap(isCache(), getCacheHint());
        }

        if (isDirty(DirtyBits.NODE_CLIP)) {
            peer.setClipNode(getClip() != null ? getClip().getPeer() : null);
        }

        if (isDirty(DirtyBits.EFFECT_EFFECT)) {
            if (getEffect() != null) {
                EffectHelper.sync(getEffect());
                peer.effectChanged();
            }
        }

        if (isDirty(DirtyBits.NODE_EFFECT)) {
            peer.setEffect(getEffect() != null ? EffectHelper.getPeer(getEffect()) : null);
        }

        if (isDirty(DirtyBits.NODE_VISIBLE)) {
            peer.setVisible(isVisible());
        }

        if (isDirty(DirtyBits.NODE_DEPTH_TEST)) {
            peer.setDepthTest(isDerivedDepthTest());
        }

        if (isDirty(DirtyBits.NODE_BLENDMODE)) {
            BlendMode mode = getBlendMode();
            peer.setNodeBlendMode((mode == null)
                                  ? null
                                  : EffectHelper.getToolkitBlendMode(mode));
        }
    }

    /* ***********************************************************************
    *                                                                        *
    *                                                                        *
    *                                                                        *
    *************************************************************************/

    private static final Object USER_DATA_KEY = new Object();
    // A map containing a set of properties for this node
    private ObservableMap<Object, Object> properties;

    /**
     * Returns an observable map of properties on this node for use primarily by application developers.
     *
     * @return an observable map of properties on this node for use primarily by application developers
     * @apiNote Layout managers use this map as well to specify layout constraints on the node, such as {@code HBox#setHgrow}, so the
     *          developer should be mindful of clearing the map or overriding its values. These entries are not removed automatically
     *          if the node is removed from the layout manager, so unused entries can exist throughout the life of the node.
     */
     public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableMap(new HashMap<>());
        }
        return properties;
    }

    /**
     * Tests if Node has properties.
     * @return true if node has properties.
     */
     public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by calling {@link Node#getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@link Node#getUserData()}.
     */
    public void setUserData(Object value) {
        getProperties().put(USER_DATA_KEY, value);
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@link Node#setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     */
    public Object getUserData() {
        return getProperties().get(USER_DATA_KEY);
    }

    /* ************************************************************************
     *                                                                        *
     *
     *                                                                        *
     *************************************************************************/

    /**
     * The parent of this {@code Node}. If this {@code Node} has not been added
     * to a scene graph, then parent will be null.
     *
     * @defaultValue null
     */
    private ReadOnlyObjectWrapper<Parent> parent;

    final void setParent(Parent value) {
        parentPropertyImpl().set(value);
    }

    public final Parent getParent() {
        return parent == null ? null : parent.get();
    }

    public final ReadOnlyObjectProperty<Parent> parentProperty() {
        return parentPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Parent> parentPropertyImpl() {
        if (parent == null) {
            parent = new ReadOnlyObjectWrapper<>() {
                private Parent oldParent;

                @Override
                protected void invalidated() {
                    if (oldParent != null) {
                        if (nodeTransformation != null && nodeTransformation.listenerReasons > 0) {
                            ((Node) oldParent).localToSceneTransformProperty().removeListener(
                                    nodeTransformation.getLocalToSceneInvalidationListener());
                        }
                    }
                    updateDisabled();
                    computeDerivedDepthTest();
                    resetInitialCssStateFlag();

                    final Parent newParent = get();

                    // Update the focus bits before calling reapplyCss(), as the focus bits can affect CSS styling.
                    updateParentsFocusWithin(oldParent, newParent);

                    if (newParent != null) {
                        if (nodeTransformation != null && nodeTransformation.listenerReasons > 0) {
                            ((Node) newParent).localToSceneTransformProperty().addListener(
                                    nodeTransformation.getLocalToSceneInvalidationListener());
                        }
                        //
                        // if parent changed, then CSS needs to be reapplied so
                        // that this node will get the right styles. This used
                        // to be done from Parent.children's onChanged method.
                        // See the comments there, also.
                        //
                        reapplyCSS();
                    } else {
                        // JDK-8123224: reset CssFlag to clean so css will be reapplied if the node is added back later.
                        // If flag is REAPPLY, then reapplyCSS() will just return and the call to
                        // notifyParentsOfInvalidatedCSS() will be skipped thus leaving the node un-styled.
                        cssFlag = CssFlags.CLEAN;
                    }
                    updateTreeVisible(true);
                    oldParent = newParent;
                    invalidateLocalToSceneTransform();
                    parentResolvedOrientationInvalidated();
                    notifyAccessibleAttributeChanged(AccessibleAttribute.PARENT);
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "parent";
                }
            };
        }
        return parent;
    }

    private SubScene subScene = null;

    /**
     * The {@link Scene} that this {@code Node} is part of. If the Node is not
     * part of a scene, then this variable will be null.
     *
     * @defaultValue null
     */
    private ReadOnlyObjectWrapperManualFire<Scene> scene = new ReadOnlyObjectWrapperManualFire<>();

    private class ReadOnlyObjectWrapperManualFire<T> extends ReadOnlyObjectWrapper<T> {
        @Override
        public Object getBean() {
            return Node.this;
        }

        @Override
        public String getName() {
            return "scene";
        }

        @Override
        protected void fireValueChangedEvent() {
            /*
             * Note: This method has been intentionally made into a no-op. In
             * order to override the default set behavior. By default calling
             * set(...) on a different scene will trigger:
             * - invalidated();
             * - fireValueChangedEvent();
             * Both of the above are no-ops, but are handled manually via
             * - Node.this.setScenes(...)
             * - Node.this.invalidatedScenes(...)
             * - forceValueChangedEvent()
             */
        }

        public void fireSuperValueChangedEvent() {
            super.fireValueChangedEvent();
        }
    }

    private void invalidatedScenes(Scene oldScene, SubScene oldSubScene) {
        Scene newScene = sceneProperty().get();
        boolean sceneChanged = oldScene != newScene;
        SubScene newSubScene = subScene;

        if (getClip() != null) {
            getClip().setScenes(newScene, newSubScene);
        }
        if (sceneChanged) {
            if (oldScene != null) {
                oldScene.unregisterClearInitialCssStageFlag(this);
            }

            if (newScene == null) {
                completeTransitionTimers();
            } else {
                resetInitialCssStateFlag();
            }
            updateCanReceiveFocus();
            if (isFocusTraversable()) {
                if (newScene != null) {
                    newScene.initializeInternalEventDispatcher();
                }
            }
            focusSetDirty(oldScene);
            focusSetDirty(newScene);
        }
        scenesChanged(newScene, newSubScene, oldScene, oldSubScene);

        if (sceneChanged) reapplyCSS();

        if (sceneChanged && !isDirtyEmpty()) {
            //Note: no need to remove from scene's dirty list
            //Scene's is checking if the node's scene is correct
            /* TODO: looks like an existing bug when a node is moved from one
             * location to another, setScenes will be called twice by
             * Parent.VetoableListDecorator onProposedChange and onChanged
             * respectively. Removing the node and setting setScense(null,null)
             * then adding it back to potentially the same scene. Causing the
             * same node to being added twice to the same scene.
             */
            addToSceneDirtyList();
        }

        if (newScene == null && peer != null) {
            peer.release();
        }

        if (oldScene != null) {
            oldScene.clearNodeMnemonics(this);
        }

        if (getParent() == null || isInheritOrientationFromScene(resolvedNodeOrientation)) {
            parentResolvedOrientationInvalidated();
        }

        if (sceneChanged) { scene.fireSuperValueChangedEvent(); }

        /* Dispose the accessible peer, if any. If AT ever needs this node again
         * a new accessible peer is created. */
        if (accessible != null) {
            /* Generally accessibility does not retain any state, therefore deleting objects
             * generally does not cause problems (AT just asks everything back).
             * The exception to this rule is when the object sends a notifications to the AT,
             * in which case it is expected to be around to answer request for the new values.
             * It is possible that a object is reparented (within the scene) in the middle of
             * this process. For example, when a tree item is expanded, the notification is
             * sent to the AT by the cell. But when the TreeView relayouts the cell can be
             * reparented before AT can query the relevant information about the expand event.
             * If the accessible was disposed, AT can't properly report the event.
             *
             * The fix is to defer the disposal of the accessible to the next pulse.
             * If at that time the node is placed back to the scene, then the accessible is hooked
             * to Node and AT requests are processed. Otherwise the accessible is disposed.
             */
            if (oldScene != null && oldScene != newScene && newScene == null) {
                // Strictly speaking we need some type of accessible.thaw() at this point.
                oldScene.addAccessible(Node.this, accessible);
            } else {
                accessible.dispose();
            }
            /* Always set to null to ensure this accessible is never on more than one
             * Scene#accMap at the same time (At lest not with the same accessible).
             */
            accessible = null;
        }
    }

    final void setScenes(Scene newScene, SubScene newSubScene) {
        Scene oldScene = sceneProperty().get();
        if (newScene != oldScene || newSubScene != subScene) {
            scene.set(newScene);
            SubScene oldSubScene = subScene;
            subScene = newSubScene;
            invalidatedScenes(oldScene, oldSubScene);
            if (this instanceof SubScene) { // TODO: find better solution
                SubScene thisSubScene = (SubScene)this;
                thisSubScene.getRoot().setScenes(newScene, thisSubScene);
            }
        }
    }

    final SubScene getSubScene() {
        return subScene;
    }

    public final Scene getScene() {
        return scene.get();
    }

    public final ReadOnlyObjectProperty<Scene> sceneProperty() {
        return scene.getReadOnlyProperty();
    }

    /**
     * Exists for Parent and LightBase
     */
    void scenesChanged(final Scene newScene, final SubScene newSubScene,
                       final Scene oldScene, final SubScene oldSubScene) { }


    /**
     * The id of this {@code Node}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     * <p>
     *     For example, if a Node is given the id of "myId", then the lookup method can
     *     be used to find this node as follows: <code>scene.lookup("#myId");</code>.
     * </p>
     *
     * @defaultValue null
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     */
    private StringProperty id;

    public final void setId(String value) {
        idProperty().set(value);
    }

    //TODO: this is copied from the property in order to add the @return statement.
    //      We should have a better, general solution without the need to copy it.
    /**
     * The id of this {@code Node}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     *
     * @return the id assigned to this {@code Node} using the {@code setId}
     *         method or {@code null}, if no id has been assigned.
     * @defaultValue null
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>
     */
    @Override
    public final String getId() {
        return id == null ? null : id.get();
    }

    public final StringProperty idProperty() {
        if (id == null) {
            id = new StringPropertyBase() {

                @Override
                protected void invalidated() {
                    reapplyCSS();
                    if (PrismSettings.printRenderGraph) {
                        NodeHelper.markDirty(Node.this, DirtyBits.DEBUG);
                    }
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "id";
                }
            };
        }
        return id;
    }

    /**
     * A list of String identifiers which can be used to logically group
     * Nodes, specifically for an external style engine. This variable is
     * analogous to the "class" attribute on an HTML element and, as such,
     * each element of the list is a style class to which this Node belongs.
     *
     * @see <a href="http://www.w3.org/TR/css3-selectors/#class-html">CSS3 class selectors</a>
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     * @defaultValue null
     */
    private ObservableList<String> styleClass = new TrackableObservableList<>() {
        @Override
        protected void onChanged(Change<String> c) {
            reapplyCSS();
        }

        @Override
        public String toString() {
            if (size() == 0) {
                return "";
            } else if (size() == 1) {
                return get(0);
            } else {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < size(); i++) {
                    buf.append(get(i));
                    if (i + 1 < size()) {
                        buf.append(' ');
                    }
                }
                return buf.toString();
            }
        }
    };

    @Override
    public final ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /**
     * A string representation of the CSS style associated with this
     * specific {@code Node}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @defaultValue empty string
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     */
    private StringProperty style;

    /**
     * A string representation of the CSS style associated with this
     * specific {@code Node}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @param value The inline CSS style to use for this {@code Node}.
     *         {@code null} is implicitly converted to an empty String.
     * @defaultValue empty string
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>
     */
    public final void setStyle(String value) {
        styleProperty().set(value);
    }

    // TODO: javadoc copied from property for the sole purpose of providing a return tag
    /**
     * A string representation of the CSS style associated with this
     * specific {@code Node}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @defaultValue empty string
     * @return The inline CSS style associated with this {@code Node}.
     *         If this {@code Node} does not have an inline style,
     *         an empty String is returned.
     * @see <a href="doc-files/cssref.html">CSS Reference Guide</a>
     */
    @Override
    public final String getStyle() {
        return style == null ? "" : style.get();
    }

    public final StringProperty styleProperty() {
        if (style == null) {
            style = new StringPropertyBase("") {

                @Override public void set(String value) {
                    // getStyle returns an empty string if the style property
                    // is null. To be consistent, getStyle should also return
                    // an empty string when the style property's value is null.
                    super.set((value != null) ? value : "");
                }

                @Override
                protected void invalidated() {
                    // If the style has changed, then styles of this node
                    // and child nodes might be affected.
                    reapplyCSS();
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "style";
                }
            };
        }
        return style;
    }

    /**
     * Specifies whether this {@code Node} and any subnodes should be rendered
     * as part of the scene graph. A node may be visible and yet not be shown
     * in the rendered scene if, for instance, it is off the screen or obscured
     * by another Node. Invisible nodes never receive mouse events or
     * keyboard focus and never maintain keyboard focus when they become
     * invisible.
     *
     * @defaultValue true
     */
    private BooleanProperty visible;

    public final void setVisible(boolean value) {
        visibleProperty().set(value);
    }

    public final boolean isVisible() {
        return visible == null ? true : visible.get();
    }

    public final BooleanProperty visibleProperty() {
        if (visible == null) {
            visible = new StyleableBooleanProperty(true) {
                boolean oldValue = true;
                @Override
                protected void invalidated() {
                    if (oldValue != get()) {
                        NodeHelper.markDirty(Node.this, DirtyBits.NODE_VISIBLE);
                        NodeHelper.geomChanged(Node.this);
                        updateTreeVisible(false);
                        if (getParent() != null) {
                            // notify the parent of the potential change in visibility
                            // of this node, since visibility affects bounds of the
                            // parent node
                            getParent().childVisibilityChanged(Node.this);
                        }
                        oldValue = get();
                    }
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.VISIBILITY;
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "visible";
                }
            };
        }
        return visible;
    }

    public final void setCursor(Cursor value) {
        cursorProperty().set(value);
    }

    public final Cursor getCursor() {
        return (miscProperties == null) ? DEFAULT_CURSOR
                                        : miscProperties.getCursor();
    }

    /**
     * Defines the mouse cursor for this {@code Node} and subnodes. If null,
     * then the cursor of the first parent node with a non-null cursor will be
     * used. If no Node in the scene graph defines a cursor, then the cursor
     * of the {@code Scene} will be used.
     *
     * @return the mouse cursor for this {@code Node} and subnodes
     * @defaultValue null
     */
    public final ObjectProperty<Cursor> cursorProperty() {
        return getMiscProperties().cursorProperty();
    }

    /**
     * Specifies how opaque (that is, solid) the {@code Node} appears. A Node
     * with 0% opacity is fully translucent. That is, while it is still
     * {@link #visibleProperty visible} and rendered, you generally won't be able to see it. The
     * exception to this rule is when the {@code Node} is combined with a
     * blending mode and blend effect in which case a translucent Node may still
     * have an impact in rendering. An opacity of 50% will render the node as
     * being 50% transparent.
     * <p>
     * A {@link #visibleProperty visible} node with any opacity setting still receives mouse
     * events and can receive keyboard focus. For example, if you want to have
     * a large invisible rectangle overlay all {@code Node}s in the scene graph
     * in order to intercept mouse events but not be visible to the user, you could
     * create a large {@code Rectangle} that had an opacity of 0%.
     * <p>
     * Opacity is specified as a value between 0 and 1. Values less than 0 are
     * treated as 0, values greater than 1 are treated as 1.
     * <p>
     * On some platforms ImageView might not support opacity variable.
     *
     * <p>
     * There is a known limitation of mixing opacity &lt; 1.0 with a 3D Transform.
     * Opacity/Blending is essentially a 2D image operation. The result of
     * an opacity &lt; 1.0 set on a {@link Group} node with 3D transformed children
     * will cause its children to be rendered in order without Z-buffering
     * applied between those children.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty opacity;

    public final void setOpacity(double value) {
        opacityProperty().set(value);
    }
    public final double getOpacity() {
        return opacity == null ? 1 : opacity.get();
    }

    public final DoubleProperty opacityProperty() {
        if (opacity == null) {
            opacity = new StyleableDoubleProperty(1) {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Node.this, DirtyBits.NODE_OPACITY);
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.OPACITY;
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "opacity";
                }
            };
        }
        return opacity;
    }

    /**
     * The {@link javafx.scene.effect.BlendMode} used to blend this individual node
     * into the scene behind it. If this node is a {@code Group}, then all of the
     * children will be composited individually into a temporary buffer using their
     * own blend modes and then that temporary buffer will be composited into the
     * scene using the specified blend mode.
     *
     * A value of {@code null} is treated as pass-through. This means no effect on a
     * parent (such as a {@code Group}), and the equivalent of {@code SRC_OVER} for a single {@code Node}.
     *
     * @defaultValue {@code null}
     */
    private javafx.beans.property.ObjectProperty<BlendMode> blendMode;

    public final void setBlendMode(BlendMode value) {
        blendModeProperty().set(value);
    }
    public final BlendMode getBlendMode() {
        return blendMode == null ? null : blendMode.get();
    }

    public final ObjectProperty<BlendMode> blendModeProperty() {
        if (blendMode == null) {
            blendMode = new StyleableObjectProperty<BlendMode>(null) {
                @Override public void invalidated() {
                    NodeHelper.markDirty(Node.this, DirtyBits.NODE_BLENDMODE);
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.BLEND_MODE;
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "blendMode";
                }
            };
        }
        return blendMode;
    }

    public final void setClip(Node value) {
        clipProperty().set(value);
    }

    public final Node getClip() {
        return (miscProperties == null) ? DEFAULT_CLIP
                                        : miscProperties.getClip();
    }

    /**
     * Specifies a {@code Node} to use to define the clipping shape for this
     * Node. This clipping Node is not a child of this {@code Node} in the scene
     * graph sense. Rather, it is used to define the clip for this {@code Node}.
     * <p>
     * For example, you can use an {@link javafx.scene.image.ImageView} Node as
     * a mask to represent the Clip. Or you could use one of the geometric shape
     * Nodes such as {@link javafx.scene.shape.Rectangle} or
     * {@link javafx.scene.shape.Circle}. Or you could use a
     * {@link javafx.scene.text.Text} node to represent the Clip.
     * <p>
     * See the class documentation for {@link Node} for scene graph structure
     * restrictions on setting the clip. If these restrictions are violated by
     * a change to the clip variable, the change is ignored and the
     * previous value of the clip variable is restored.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SHAPE_CLIP ConditionalFeature.SHAPE_CLIP}
     * for more information.
     * <p>
     * There is a known limitation of mixing Clip with a 3D Transform.
     * Clipping is essentially a 2D image operation. The result of
     * a Clip set on a {@link Group} node with 3D transformed children
     * will cause its children to be rendered in order without Z-buffering
     * applied between those children.
     *
     * @return the the clipping shape for this {@code Node}
     * @defaultValue null
     */
    public final ObjectProperty<Node> clipProperty() {
        return getMiscProperties().clipProperty();
    }

    public final void setCache(boolean value) {
        cacheProperty().set(value);
    }

    public final boolean isCache() {
        return (miscProperties == null) ? DEFAULT_CACHE
                                        : miscProperties.isCache();
    }

    /**
     * A performance hint to the system to indicate that this {@code Node}
     * should be cached as a bitmap. Rendering a bitmap representation of a node
     * will be faster than rendering primitives in many cases, especially in the
     * case of primitives with effects applied (such as a blur). However, it
     * also increases memory usage. This hint indicates whether that trade-off
     * (increased memory usage for increased performance) is worthwhile. Also
     * note that on some platforms such as GPU accelerated platforms there is
     * little benefit to caching Nodes as bitmaps when blurs and other effects
     * are used since they are very fast to render on the GPU.
     *
     * The {@link #cacheHintProperty} variable provides additional options for enabling
     * more aggressive bitmap caching.
     *
     * <p>
     * Caching may be disabled for any node that has a 3D transform on itself,
     * any of its ancestors, or any of its descendants.
     *
     * @return the hint to cache for this {@code Node}
     * @see #cacheHintProperty
     * @defaultValue false
     */
    public final BooleanProperty cacheProperty() {
        return getMiscProperties().cacheProperty();
    }

    public final void setCacheHint(CacheHint value) {
        cacheHintProperty().set(value);
    }

    public final CacheHint getCacheHint() {
        return (miscProperties == null) ? DEFAULT_CACHE_HINT
                                        : miscProperties.getCacheHint();
    }

    /**
     * Additional hint for controlling bitmap caching.
     * <p>
     * Under certain circumstances, such as animating nodes that are very
     * expensive to render, it is desirable to be able to perform
     * transformations on the node without having to regenerate the cached
     * bitmap.  An option in such cases is to perform the transforms on the
     * cached bitmap itself.
     * <p>
     * This technique can provide a dramatic improvement to animation
     * performance, though may also result in a reduction in visual quality.
     * The {@code cacheHint} variable provides a hint to the system about how
     * and when that trade-off (visual quality for animation performance) is
     * acceptable.
     * <p>
     * It is possible to enable the cacheHint only at times when your node is
     * animating.  In this way, expensive nodes can appear on screen with full
     * visual quality, yet still animate smoothly.
     * <p>
     * Example:
     * <pre>{@code
        expensiveNode.setCache(true);
        expensiveNode.setCacheHint(CacheHint.QUALITY);
        ...
        // Do an animation
        expensiveNode.setCacheHint(CacheHint.SPEED);
        new Timeline(
            new KeyFrame(Duration.seconds(2),
                new KeyValue(expensiveNode.scaleXProperty(), 2.0),
                new KeyValue(expensiveNode.scaleYProperty(), 2.0),
                new KeyValue(expensiveNode.rotateProperty(), 360),
                new KeyValue(expensiveNode.cacheHintProperty(), CacheHint.QUALITY)
            )
        ).play();
     }</pre>
     *
     * Note that {@code cacheHint} is only a hint to the system.  Depending on
     * the details of the node or the transform, this hint may be ignored.
     *
     * <p>
     * If {@code Node.cache} is false, cacheHint is ignored.
     * Caching may be disabled for any node that has a 3D transform on itself,
     * any of its ancestors, or any of its descendants.
     *
     * @return the {@code CacheHint} for this {@code Node}
     * @see #cacheProperty
     * @defaultValue CacheHint.DEFAULT
     */
    public final ObjectProperty<CacheHint> cacheHintProperty() {
        return getMiscProperties().cacheHintProperty();
    }

    public final void setEffect(Effect value) {
        effectProperty().set(value);
    }

    public final Effect getEffect() {
        return (miscProperties == null) ? DEFAULT_EFFECT
                                        : miscProperties.getEffect();
    }

    /**
     * Specifies an effect to apply to this {@code Node}.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#EFFECT ConditionalFeature.EFFECT}
     * for more information.
     *
     * <p>
     * There is a known limitation of mixing Effect with a 3D Transform. Effect is
     * essentially a 2D image operation. The result of an Effect set on
     * a {@link Group} node with 3D transformed children will cause its children
     * to be rendered in order without Z-buffering applied between those
     * children.
     *
     * @return the effect for this {@code Node}
     * @defaultValue null
     */
    public final ObjectProperty<Effect> effectProperty() {
        return getMiscProperties().effectProperty();
    }

    public final void setDepthTest(DepthTest value) {
        depthTestProperty().set(value);
    }

    public final DepthTest getDepthTest() {
        return (miscProperties == null) ? DEFAULT_DEPTH_TEST
                                        : miscProperties.getDepthTest();
    }

    /**
     * Indicates whether depth testing is used when rendering this node.
     * If the depthTest flag is {@code DepthTest.DISABLE}, then depth testing
     * is disabled for this node.
     * If the depthTest flag is {@code DepthTest.ENABLE}, then depth testing
     * is enabled for this node.
     * If the depthTest flag is {@code DepthTest.INHERIT}, then depth testing
     * is enabled for this node if it is enabled for the parent node or the
     * parent node is null.
     * <p>
     * The depthTest flag is only used when the depthBuffer flag for
     * the {@link Scene} is true (meaning that the
     * {@link Scene} has an associated depth buffer)
     * <p>
     * Depth test comparison is only done among nodes with depthTest enabled.
     * A node with depthTest disabled does not read, test, or write the depth buffer,
     * that is to say its Z value will not be considered for depth testing
     * with other nodes.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     * <p>
     * See the constructor in Scene with depthBuffer as one of its input
     * arguments.
     *
     * @return the depth test setting for this {@code Node}
     * @see javafx.scene.Scene
     * @defaultValue INHERIT
     */
    public final ObjectProperty<DepthTest> depthTestProperty() {
        return getMiscProperties().depthTestProperty();
    }

    /**
     * Recompute the derived depth test flag. This flag is true
     * if the depthTest flag for this node is true and the
     * depth test flag for each ancestor node is true. It is false
     * otherwise. Equivalently, the derived depth flag is true
     * if the depthTest flag for this node is true and the derivedDepthTest
     * flag for its parent is true.
     */
    void computeDerivedDepthTest() {
        boolean newDDT;
        if (getDepthTest() == DepthTest.INHERIT) {
            if (getParent() != null) {
                newDDT = getParent().isDerivedDepthTest();
            } else {
                newDDT = true;
            }
        } else if (getDepthTest() == DepthTest.ENABLE) {
            newDDT = true;
        } else {
            newDDT = false;
        }

        if (isDerivedDepthTest() != newDDT) {
            NodeHelper.markDirty(this, DirtyBits.NODE_DEPTH_TEST);
            setDerivedDepthTest(newDDT);
        }
    }

    // This is the derived depthTest value to pass to PG level
    private boolean derivedDepthTest = true;

    void setDerivedDepthTest(boolean value) {
        derivedDepthTest = value;
    }

    boolean isDerivedDepthTest() {
        return derivedDepthTest;
    }

    public final void setDisable(boolean value) {
        disableProperty().set(value);
    }

    public final boolean isDisable() {
        return (miscProperties == null) ? DEFAULT_DISABLE
                                        : miscProperties.isDisable();
    }

    /**
     * Defines the individual disabled state of this {@code Node}. Setting
     * {@code disable} to true will cause this {@code Node} and any subnodes to
     * become disabled. This property should be used only to set the disabled
     * state of a {@code Node}.  For querying the disabled state of a
     * {@code Node}, the {@link #disabledProperty disabled} property should instead be used,
     * since it is possible that a {@code Node} was disabled as a result of an
     * ancestor being disabled even if the individual {@code disable} state on
     * this {@code Node} is {@code false}.
     *
     * @return the disabled state for this {@code Node}
     * @defaultValue false
     */
    public final BooleanProperty disableProperty() {
        return getMiscProperties().disableProperty();
    }


//    /**
//     * TODO document - null by default, could be non-null in subclasses (e.g. Control)
//     */
//    public final ObjectProperty<InputMap<?>> inputMapProperty() {
//        if (inputMap == null) {
//            inputMap = new SimpleObjectProperty<InputMap<?>>(this, "inputMap") {
//                private InputMap<?> currentMap = get();
//                @Override protected void invalidated() {
//                    if (currentMap != null) {
//                        currentMap.dispose();
//                    }
//                    currentMap = get();
//                }
//            };
//        }
//        return inputMap;
//    }
//    public final void setInputMap(InputMap<?> value) { inputMapProperty().set(value); }
//    public final InputMap<?> getInputMap() { return inputMapProperty().getValue(); }
//    private ObjectProperty<InputMap<?>> inputMap;


    /* ************************************************************************
     *                                                                        *
     *
     *                                                                        *
     *************************************************************************/
    /**
     * Defines how the picking computation is done for this node when
     * triggered by a {@code MouseEvent} or a {@code contains} function call.
     *
     * If {@code pickOnBounds} is {@code true}, then picking is computed by
     * intersecting with the bounds of this node, else picking is computed
     * by intersecting with the geometric shape of this node.
     *
     * The default value of this property is {@code false} unless
     * overridden by a subclass. The default value is {@code true}
     * for {@link javafx.scene.layout.Region}.
     *
     * @defaultValue false; true for {@code Region}
     */
    private BooleanProperty pickOnBounds;

    public final void setPickOnBounds(boolean value) {
        pickOnBoundsProperty().set(value);
    }

    public final boolean isPickOnBounds() {
        return pickOnBounds == null ? false : pickOnBounds.get();
    }

    public final BooleanProperty pickOnBoundsProperty() {
        if (pickOnBounds == null) {
            pickOnBounds = new SimpleBooleanProperty(this, "pickOnBounds");
        }
        return pickOnBounds;
    }

    /**
     * Indicates whether or not this {@code Node} is disabled.  A {@code Node}
     * will become disabled if {@link #disableProperty disable} is set to {@code true} on either
     * itself or one of its ancestors in the scene graph.
     * <p>
     * A disabled {@code Node} should render itself differently to indicate its
     * disabled state to the user.
     * Such disabled rendering is dependent on the implementation of the
     * {@code Node}. The shape classes contained in {@code javafx.scene.shape}
     * do not implement such rendering by default, therefore applications using
     * shapes for handling input must implement appropriate disabled rendering
     * themselves. The user-interface controls defined in
     * {@code javafx.scene.control} will implement disabled-sensitive rendering,
     * however.
     * <p>
     * A disabled {@code Node} does not receive mouse or key events.
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper disabled;

    protected final void setDisabled(boolean value) {
        disabledPropertyImpl().set(value);
    }

    public final boolean isDisabled() {
        return disabled == null ? false : disabled.get();
    }

    public final ReadOnlyBooleanProperty disabledProperty() {
        return disabledPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper disabledPropertyImpl() {
        if (disabled == null) {
            disabled = new ReadOnlyBooleanWrapper() {

                @Override
                protected void invalidated() {
                    pseudoClassStateChanged(DISABLED_PSEUDOCLASS_STATE, get());
                    updateCanReceiveFocus();
                    focusSetDirty(getScene());

                    if (Node.this instanceof Parent parent) {
                        parent.getChildren().forEach(Node::updateDisabled);
                    }
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "disabled";
                }
            };
        }
        return disabled;
    }

    private void updateDisabled() {
        boolean isDisabled = isDisable();
        if (!isDisabled) {
            isDisabled = getParent() != null ? getParent().isDisabled() :
                    getSubScene() != null && getSubScene().isDisabled();
        }
        setDisabled(isDisabled);
        if (this instanceof SubScene) {
            ((SubScene)this).getRoot().setDisabled(isDisabled);
        }
    }

    /**
     * Finds this {@code Node}, or the first sub-node, based on the given CSS selector.
     * If this node is a {@code Parent}, then this function will traverse down
     * into the branch until it finds a match. If more than one sub-node matches the
     * specified selector, this function returns the first of them.
     * <p>
     * If the lookup selector does not specify a pseudo class, the lookup will ignore pseudo class states;
     * it will return the first matching node whether or not it contains pseudo classes.
     * <p>
     * For example, if a Node is given the id of "myId", then the lookup method can
     * be used to find this node as follows: {@code scene.lookup("#myId");}.
     * <p>
     * For example, if two nodes, NodeA and NodeB, have the same style class "myStyle" and NodeA has
     * a pseudo state "myPseudo", then to find NodeA, the lookup method can be used as follows:
     * {@code scene.lookup(".myStyle:myPseudo");} or {@code scene.lookup(":myPseudo");}.
     *
     * @param selector The css selector of the node to find
     * @return The first node, starting from this {@code Node}, which matches
     *         the CSS {@code selector}, null if none is found.
     */
    public Node lookup(String selector) {
        if (selector == null) return null;
        Selector s = Selector.createSelector(selector);
        return selectorMatches(s) ? this : null;
    }

    /**
     * Finds all {@code Node}s, including this one and any children, which match
     * the given CSS selector. If no matches are found, an empty unmodifiable set is
     * returned. The set is explicitly unordered.
     * <p>
     * If the lookupAll selector does not specify a pseudo class, the lookupAll will ignore pseudo class states;
     * it will return all matching nodes whether or not the nodes contain pseudo classes.
     * <p>
     * For example, if there are multiple nodes with same style class "myStyle", then the lookupAll method can
     * be used to find all these nodes as follows: {@code scene.lookupAll(".myStyle");}.
     * <p>
     * For example, if multiple nodes have same style class "myStyle" and few nodes have
     * a pseudo state "myPseudo", then to find all nodes with "myPseudo" state, the lookupAll method can be used as follows:
     * {@code scene.lookupAll(".myStyle:myPseudo");} or {@code scene.lookupAll(":myPseudo");}.
     *
     * @param selector The css selector of the nodes to find
     * @return All nodes, starting from and including this {@code Node}, which match
     *         the CSS {@code selector}. The returned set is always unordered and
     *         unmodifiable, and never null.
     */
    public Set<Node> lookupAll(String selector) {
        final Selector s = Selector.createSelector(selector);
        final Set<Node> empty = Collections.emptySet();
        if (s == null) return empty;
        List<Node> results = lookupAll(s, null);
        return results == null ? empty : new UnmodifiableListSet<>(results);
    }

    /**
     * Used by Node and Parent for traversing the tree and adding all nodes which
     * match the given selector.
     *
     * @param selector the css selector of the nodes to find
     * @param results the results
     * @return list of matching nodes
     */
    List<Node> lookupAll(Selector selector, List<Node> results) {
        if (selectorMatches(selector)) {
            // Lazily create the set to reduce some trash.
            if (results == null) {
                results = new LinkedList<>();
            }
            results.add(this);
        }
        return results;
    }

    /**
     * Moves this {@code Node} to the back of its sibling nodes in terms of
     * z-order.  This is accomplished by moving this {@code Node} to the
     * first position in its parent's {@code content} ObservableList.
     * This function has no effect if this {@code Node} is not part of a group.
     */
    public void toBack() {
        if (getParent() != null) {
            getParent().toBack(this);
        }
    }

    /**
     * Moves this {@code Node} to the front of its sibling nodes in terms of
     * z-order.  This is accomplished by moving this {@code Node} to the
     * last position in its parent's {@code content} ObservableList.
     * This function has no effect if this {@code Node} is not part of a group.
     */
    public void toFront() {
        if (getParent() != null) {
            getParent().toFront(this);
        }
    }

    /**
     * Checks whether the provided selector matches the node with both styles and pseudo states.
     * @param s selector to match
     * @return {@code true} if the selector matches
     */
    private boolean selectorMatches(Selector s) {
        boolean matches = s != null && s.applies(this);
        if (matches && !s.createMatch().getPseudoClasses().isEmpty()) {
            matches = s.stateMatches(this, this.getPseudoClassStates());
        }
        return matches;
    }

    // TODO: need to verify whether this is OK to do starting from a node in
    // the scene graph other than the root.
    private void doCSSPass() {
        if (this.cssFlag != CssFlags.CLEAN) {
            // The dirty bit isn't checked but we must ensure it is cleared.
            // The cssFlag is set to clean in either Node.processCSS or
            // NodeHelper.processCSS

            // Don't clear the dirty bit in case it will cause problems
            // with a full CSS pass on the scene.
            // TODO: is this the right thing to do?
            // this.clearDirty(com.sun.javafx.scene.DirtyBits.NODE_CSS);

            this.processCSS();
        }
    }

    /**
     * Recursive function for synchronizing a node and all descendents
     */
    private static void syncAll(Node node) {
        node.syncPeer();
        if (node instanceof Parent) {
            Parent p = (Parent) node;
            final int childrenCount = p.getChildren().size();

            for (int i = 0; i < childrenCount; i++) {
                Node n = p.getChildren().get(i);
                if (n != null) {
                    syncAll(n);
                }
            }
        }
        if (node.getClip() != null) {
            syncAll(node.getClip());
        }
    }

    private void doLayoutPass() {
        if (this instanceof Parent) {
            // TODO: As an optimization we only need to layout those dirty
            // roots that are descendants of this node
            Parent p = (Parent)this;
            for (int i = 0; i < 3; i++) {
                p.layout();
            }
        }
    }

    private void doCSSLayoutSyncForSnapshot() {
        doCSSPass();
        doLayoutPass();
        updateBounds();
        Scene.setAllowPGAccess(true);
        syncAll(this);
        Scene.setAllowPGAccess(false);
    }

    private WritableImage doSnapshot(SnapshotParameters params, WritableImage img) {
        if (getScene() != null) {
            getScene().doCSSLayoutSyncForSnapshot(this);
        } else {
            doCSSLayoutSyncForSnapshot();
        }

        BaseTransform transform = BaseTransform.IDENTITY_TRANSFORM;
        if (params.getTransform() != null) {
            Affine3D tempTx = new Affine3D();
            TransformHelper.apply(params.getTransform(), tempTx);
            transform = tempTx;
        }
        double x;
        double y;
        double w;
        double h;
        Rectangle2D viewport = params.getViewport();
        if (viewport != null) {
            // Use the specified viewport
            x = viewport.getMinX();
            y = viewport.getMinY();
            w = viewport.getWidth();
            h = viewport.getHeight();
        } else {
            // Get the bounds in parent of this node, transformed by the
            // specified transform.
            BaseBounds tempBounds = TempState.getInstance().bounds;
            tempBounds = getTransformedBounds(tempBounds, transform);
            x = tempBounds.getMinX();
            y = tempBounds.getMinY();
            w = tempBounds.getWidth();
            h = tempBounds.getHeight();
        }
        WritableImage result = Scene.doSnapshot(getScene(), getSubScene(), x, y, w, h,
                this, transform, params.isDepthBufferInternal(),
                params.getFill(), params.getEffectiveCamera(), img);

        return result;
    }

    /**
     * Takes a snapshot of this node and returns the rendered image when
     * it is ready.
     * CSS and layout processing will be done for the node, and any of its
     * children, prior to rendering it.
     * The entire destination image is cleared to the fill {@code Paint}
     * specified by the SnapshotParameters. This node is then rendered to
     * the image.
     * If the viewport specified by the SnapshotParameters is null, the
     * upper-left pixel of the {@code boundsInParent} of this
     * node, after first applying the transform specified by the
     * SnapshotParameters,
     * is mapped to the upper-left pixel (0,0) in the image.
     * If a non-null viewport is specified,
     * the upper-left pixel of the viewport is mapped to upper-left pixel
     * (0,0) in the image.
     * In both cases, this mapping to (0,0) of the image is done with an integer
     * translation. The portion of the node that is outside of the rendered
     * image will be clipped by the image.
     *
     * <p>
     * When taking a snapshot of a scene that is being animated, either
     * explicitly by the application or implicitly (such as chart animation),
     * the snapshot will be rendered based on the state of the scene graph at
     * the moment the snapshot is taken and will not reflect any subsequent
     * animation changes.
     * </p>
     *
     * <p>
     * NOTE: In order for CSS and layout to function correctly, the node
     * must be part of a Scene (the Scene may be attached to a Stage, but need
     * not be).
     * </p>
     *
     * @param params the snapshot parameters containing attributes that
     * will control the rendering. If the SnapshotParameters object is null,
     * then the Scene's attributes will be used if this node is part of a scene,
     * or default attributes will be used if this node is not part of a scene.
     *
     * @param image the writable image that will be used to hold the rendered node.
     * It may be null in which case a new WritableImage will be constructed.
     * The new image is constructed using integer width and
     * height values that are derived either from the transformed bounds of this
     * Node or from the size of the viewport as specified in the
     * SnapShotParameters. These integer values are chosen such that the image
     * will wholly contain the bounds of this Node or the specified viewport.
     * If the image is non-null, the node will be rendered into the
     * existing image.
     * In this case, the width and height of the image determine the area
     * that is rendered instead of the width and height of the bounds or
     * viewport.
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     *
     * @return the rendered image
     * @since JavaFX 2.2
     */
    public WritableImage snapshot(SnapshotParameters params, WritableImage image) {
        Toolkit.getToolkit().checkFxUserThread();

        if (params == null) {
            params = new SnapshotParameters();
            Scene s = getScene();
            if (s != null) {
                params.setCamera(s.getEffectiveCamera());
                params.setDepthBuffer(s.isDepthBufferInternal());
                params.setFill(s.getFill());
            }
        }

        return doSnapshot(params, image);
    }

    /**
     * Takes a snapshot of this node at the next frame and calls the
     * specified callback method when the image is ready.
     * CSS and layout processing will be done for the node, and any of its
     * children, prior to rendering it.
     * The entire destination image is cleared to the fill {@code Paint}
     * specified by the SnapshotParameters. This node is then rendered to
     * the image.
     * If the viewport specified by the SnapshotParameters is null, the
     * upper-left pixel of the {@code boundsInParent} of this
     * node, after first applying the transform specified by the
     * SnapshotParameters,
     * is mapped to the upper-left pixel (0,0) in the image.
     * If a non-null viewport is specified,
     * the upper-left pixel of the viewport is mapped to upper-left pixel
     * (0,0) in the image.
     * In both cases, this mapping to (0,0) of the image is done with an integer
     * translation. The portion of the node that is outside of the rendered
     * image will be clipped by the image.
     *
     * <p>
     * This is an asynchronous call, which means that other
     * events or animation might be processed before the node is rendered.
     * If any such events modify the node, or any of its children, that
     * modification will be reflected in the rendered image (just like it
     * will also be reflected in the frame rendered to the Stage, if this node
     * is part of a live scene graph).
     * </p>
     *
     * <p>
     * When taking a snapshot of a node that is being animated, either
     * explicitly by the application or implicitly (such as chart animation),
     * the snapshot will be rendered based on the state of the scene graph at
     * the moment the snapshot is taken and will not reflect any subsequent
     * animation changes.
     * </p>
     *
     * <p>
     * NOTE: In order for CSS and layout to function correctly, the node
     * must be part of a Scene (the Scene may be attached to a Stage, but need
     * not be).
     * </p>
     *
     * @param callback a class whose call method will be called when the image
     * is ready. The SnapshotResult that is passed into the call method of
     * the callback will contain the rendered image, the source node
     * that was rendered, and a copy of the SnapshotParameters.
     * The callback parameter must not be null.
     *
     * @param params the snapshot parameters containing attributes that
     * will control the rendering. If the SnapshotParameters object is null,
     * then the Scene's attributes will be used if this node is part of a scene,
     * or default attributes will be used if this node is not part of a scene.
     *
     * @param image the writable image that will be used to hold the rendered node.
     * It may be null in which case a new WritableImage will be constructed.
     * The new image is constructed using integer width and
     * height values that are derived either from the transformed bounds of this
     * Node or from the size of the viewport as specified in the
     * SnapShotParameters. These integer values are chosen such that the image
     * will wholly contain the bounds of this Node or the specified viewport.
     * If the image is non-null, the node will be rendered into the
     * existing image.
     * In this case, the width and height of the image determine the area
     * that is rendered instead of the width and height of the bounds or
     * viewport.
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     *
     * @throws NullPointerException if the callback parameter is null.
     * @since JavaFX 2.2
     */
    public void snapshot(Callback<SnapshotResult, Void> callback,
            SnapshotParameters params, WritableImage image) {

        Toolkit.getToolkit().checkFxUserThread();
        if (callback == null) {
            throw new NullPointerException("The callback must not be null");
        }

        if (params == null) {
            params = new SnapshotParameters();
            Scene s = getScene();
            if (s != null) {
                params.setCamera(s.getEffectiveCamera());
                params.setDepthBuffer(s.isDepthBufferInternal());
                params.setFill(s.getFill());
            }
        } else {
            params = params.copy();
        }

        final SnapshotParameters theParams = params;
        final Callback<SnapshotResult, Void> theCallback = callback;
        final WritableImage theImage = image;

        // Create a deferred runnable that will be run from a pulse listener
        // that is called after all of the scenes have been synced but before
        // any of them have been rendered.
        final Runnable snapshotRunnable = () -> {
            WritableImage img = doSnapshot(theParams, theImage);
            SnapshotResult result = new SnapshotResult(img, Node.this, theParams);
//                System.err.println("Calling snapshot callback");
            try {
                Void v = theCallback.call(result);
            } catch (Throwable th) {
                System.err.println("Exception in snapshot callback");
                th.printStackTrace(System.err);
            }
        };

//        System.err.println("Schedule a snapshot in the future");
        Scene.addSnapshotRunnable(snapshotRunnable);
    }

    /* ************************************************************************
     *                                                                        *
     *
     *                                                                        *
     *************************************************************************/

    public final void setOnDragEntered(
            EventHandler<? super DragEvent> value) {
        onDragEnteredProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragEntered() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragEntered();
    }

    /**
     * Defines a function to be called when drag gesture
     * enters this {@code Node}.
     * @return the event handler that is called when drag gesture enters this
     * {@code Node}
     */
    public final ObjectProperty<EventHandler<? super DragEvent>>
            onDragEnteredProperty() {
        return getEventHandlerProperties().onDragEnteredProperty();
    }

    public final void setOnDragExited(
            EventHandler<? super DragEvent> value) {
        onDragExitedProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragExited() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragExited();
    }

    /**
     * Defines a function to be called when drag gesture
     * exits this {@code Node}.
     * @return the event handler that is called when drag gesture exits this
     * {@code Node}
     */
    public final ObjectProperty<EventHandler<? super DragEvent>>
            onDragExitedProperty() {
        return getEventHandlerProperties().onDragExitedProperty();
    }

    public final void setOnDragOver(
            EventHandler<? super DragEvent> value) {
        onDragOverProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragOver() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragOver();
    }

    /**
     * Defines a function to be called when drag gesture progresses within
     * this {@code Node}.
     * @return the event handler that is called when drag gesture progresses
     * within this {@code Node}
     */
    public final ObjectProperty<EventHandler<? super DragEvent>>
            onDragOverProperty() {
        return getEventHandlerProperties().onDragOverProperty();
    }

    // Do we want DRAG_TRANSFER_MODE_CHANGED event?
//    public final void setOnDragTransferModeChanged(
//            EventHandler<? super DragEvent> value) {
//        onDragTransferModeChangedProperty().set(value);
//    }
//
//    public final EventHandler<? super DragEvent> getOnDragTransferModeChanged() {
//        return (eventHandlerProperties == null)
//                ? null : eventHandlerProperties.getOnDragTransferModeChanged();
//    }
//
//    /**
//     * Defines a function to be called this {@code Node} if it is a potential
//     * drag-and-drop target when the user takes action to change the intended
//     * {@code TransferMode}.
//     * The user can change the intended {@link TransferMode} by holding down
//     * or releasing key modifiers.
//     */
//    public final ObjectProperty<EventHandler<? super DragEvent>>
//            onDragTransferModeChangedProperty() {
//        return getEventHandlerProperties().onDragTransferModeChangedProperty();
//    }

    public final void setOnDragDropped(
            EventHandler<? super DragEvent> value) {
        onDragDroppedProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragDropped() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragDropped();
    }

    /**
     * Defines a function to be called when the mouse button is released
     * on this {@code Node} during drag and drop gesture. Transfer of data from
     * the {@link DragEvent}'s {@link DragEvent#getDragboard() dragboard} should
     * happen in this function.
     * @return the event handler that is called when the mouse button is
     * released on this {@code Node}
     */
    public final ObjectProperty<EventHandler<? super DragEvent>>
            onDragDroppedProperty() {
        return getEventHandlerProperties().onDragDroppedProperty();
    }

    public final void setOnDragDone(
            EventHandler<? super DragEvent> value) {
        onDragDoneProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragDone() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragDone();
    }

    /**
     * Defines a function to be called when this {@code Node} is a
     * drag and drop gesture source after its data has
     * been dropped on a drop target. The {@code transferMode} of the
     * event shows what just happened at the drop target.
     * If {@code transferMode} has the value {@code MOVE}, then the source can
     * clear out its data. Clearing the source's data gives the appropriate
     * appearance to a user that the data has been moved by the drag and drop
     * gesture. A {@code transferMode} that has the value {@code NONE}
     * indicates that no data was transferred during the drag and drop gesture.
     * @return the event handler that is called when this {@code Node} is a drag
     * and drop gesture source after its data has been dropped on a drop target
     */
    public final ObjectProperty<EventHandler<? super DragEvent>>
            onDragDoneProperty() {
        return getEventHandlerProperties().onDragDoneProperty();
    }

    /**
     * Confirms a potential drag and drop gesture that is recognized over this
     * {@code Node}.
     * Can be called only from a DRAG_DETECTED event handler. The returned
     * {@link Dragboard} is used to transfer data during
     * the drag and drop gesture. Placing this {@code Node}'s data on the
     * {@link Dragboard} also identifies this {@code Node} as the source of
     * the drag and drop gesture.
     * More detail about drag and drop gestures is described in the overivew
     * of {@link DragEvent}.
     *
     * @see DragEvent
     * @param transferModes The supported {@code TransferMode}(s) of this {@code Node}
     * @return A {@code Dragboard} to place this {@code Node}'s data on
     * @throws IllegalStateException if drag and drop cannot be started at this
     * moment (it's called outside of {@code DRAG_DETECTED} event handling or
     * this node is not in scene).
     */
    public Dragboard startDragAndDrop(TransferMode... transferModes) {
        if (getScene() != null) {
            return getScene().startDragAndDrop(this, transferModes);
        }

        throw new IllegalStateException("Cannot start drag and drop on node "
                + "that is not in scene");
    }

    /**
     * Starts a full press-drag-release gesture with this node as gesture
     * source. This method can be called only from a {@code DRAG_DETECTED} mouse
     * event handler. More detail about dragging gestures can be found
     * in the overview of {@link MouseEvent} and {@link MouseDragEvent}.
     *
     * @see MouseEvent
     * @see MouseDragEvent
     * @throws IllegalStateException if the full press-drag-release gesture
     * cannot be started at this moment (it's called outside of
     * {@code DRAG_DETECTED} event handling or this node is not in scene).
     * @since JavaFX 2.1
     */
    public void startFullDrag() {
        if (getScene() != null) {
            getScene().startFullDrag(this);
            return;
        }

        throw new IllegalStateException("Cannot start full drag on node "
                + "that is not in scene");
    }

    //--------------------------
    //  Private Implementation
    //--------------------------

    /**
     * If this Node is being used as the clip of another Node, that other node
     * is referred to as the clipParent. If the boundsInParent of this Node
     * changes, it must update the clipParent's bounds as well.
     */
    private Node clipParent;
    // Use a getter function instead of giving clipParent package access,
    // so that clipParent doesn't get turned into a Location.
    final Node getClipParent() {
        return clipParent;
    }

    /**
     * Determines whether this node is connected anywhere in the scene graph.
     */
    boolean isConnected() {
        // don't need to check scene, because if scene is non-null
        // parent must also be non-null
        return getParent() != null || clipParent != null;
    }

    /**
     * Tests whether creating a parent-child relationship between these
     * nodes would cause a cycle. The parent relationship includes not only
     * the "real" parent (child of Group) but also the clipParent.
     */
    boolean wouldCreateCycle(Node parent, Node child) {
        if (child != null && child.getClip() == null && (!(child instanceof Parent))) {
            return false;
    }

        Node n = parent;
        while (n != child) {
            if (n.getParent() != null) {
                n = n.getParent();
            } else if (n.getSubScene() != null) {
                n = n.getSubScene();
            } else if (n.clipParent != null) {
                n = n.clipParent;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * The peer node created by the graphics Toolkit/Pipeline implementation
     */
    private NGNode peer;

    <P extends NGNode> P getPeer() {
        if (Utils.assertionEnabled()) {
            // Assertion checking code
            if (getScene() != null && !Scene.isPGAccessAllowed()) {
                java.lang.System.err.println();
                java.lang.System.err.println("*** unexpected PG access");
                java.lang.Thread.dumpStack();
            }
        }

        if (peer == null) {
            //if (PerformanceTracker.isLoggingEnabled()) {
            //    PerformanceTracker.logEvent("Creating NGNode for [{this}, id=\"{id}\"]");
            //}
            peer = NodeHelper.createPeer(this);
            //if (PerformanceTracker.isLoggingEnabled()) {
            //    PerformanceTracker.logEvent("NGNode created");
            //}
        }
        return (P) peer;
    }

    /* *************************************************************************
     *                                                                         *
     *                              Initialization                             *
     *                                                                         *
     *  To Note limit the number of bounds computations and improve startup    *
     *  performance.                                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new instance of Node.
     */
    protected Node() {}

    /* *************************************************************************
     *                                                                         *
     * Layout related APIs.                                                    *
     *                                                                         *
     **************************************************************************/
    /**
     * Defines whether or not this node's layout will be managed by its parent.
     * If the node is managed, its parent will factor the node's geometry
     * into its own preferred size and {@link #layoutBoundsProperty layoutBounds}
     * calculations and will lay it
     * out during the scene's layout pass.  If a managed node's layoutBounds
     * changes, it will automatically trigger relayout up the scene-graph
     * to the nearest layout root (which is typically the scene's root node).
     * <p>
     * If the node is unmanaged, its parent will ignore the child in both preferred
     * size computations and layout.   Changes in layoutBounds will not trigger
     * relayout above it.   If an unmanaged node is of type {@link javafx.scene.Parent Parent},
     * it will act as a "layout root", meaning that calls to {@link Parent#requestLayout()}
     * beneath it will cause only the branch rooted by the node to be relayed out,
     * thereby isolating layout changes to that root and below.  It's the application's
     * responsibility to set the size and position of an unmanaged node.
     * <p>
     * By default all nodes are managed.
     * </p>
     *
     * @see #isResizable()
     * @see #layoutBoundsProperty()
     * @see Parent#requestLayout()
     *
     */
    private BooleanProperty managed;

    public final void setManaged(boolean value) {
        managedProperty().set(value);
    }

    public final boolean isManaged() {
        return managed == null || managed.get();
    }

    public final BooleanProperty managedProperty() {
        if (managed == null) {
            managed = new StyleableBooleanProperty(true) {

                @Override
                protected void invalidated() {
                    final Parent parent = getParent();
                    if (parent != null) {
                        parent.managedChildChanged();
                    }
                    notifyManagedChanged();
                }

                @Override
                public CssMetaData<Node, Boolean> getCssMetaData() {
                    return StyleableProperties.MANAGED;
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "managed";
                }

            };
        }
        return managed;
    }

    /**
     * Called whenever the "managed" flag has changed. This is only
     * used by Parent as an optimization to keep track of whether a
     * Parent node is a layout root or not.
     */
    void notifyManagedChanged() { }

    /**
     * Defines the x coordinate of the translation that is added to this {@code Node}'s
     * transform for the purpose of layout. The value should be computed as the
     * offset required to adjust the position of the node from its current
     * {@link #layoutBoundsProperty() layoutBounds minX} position (which might not be 0) to the desired location.
     *
     * <p>For example, if {@code textnode} should be positioned at {@code finalX}
     * <pre>{@code
     *     textnode.setLayoutX(finalX - textnode.getLayoutBounds().getMinX());
     * }</pre>
     * <p>
     * Failure to subtract {@code layoutBounds minX} may result in misplacement
     * of the node.  The {@link #relocate(double, double) relocate(x, y)} method will automatically do the
     * correct computation and should generally be used over setting layoutX directly.
     * <p>
     * The node's final translation will be computed as {@code layoutX} + {@link #translateXProperty translateX},
     * where {@code layoutX} establishes the node's stable position
     * and {@code translateX} optionally makes dynamic adjustments to that
     * position.
     * <p>
     * If the node is managed and has a {@link javafx.scene.layout.Region}
     * as its parent, then the layout region will set {@code layoutX} according to its
     * own layout policy.   If the node is unmanaged or parented by a {@link Group},
     * then the application may set {@code layoutX} directly to position it.
     *
     * @see #relocate(double, double)
     * @see #layoutBoundsProperty()
     *
     */
    private DoubleProperty layoutX;

    public final void setLayoutX(double value) {
        layoutXProperty().set(value);
    }

    public final double getLayoutX() {
        return layoutX == null ? 0.0 : layoutX.get();
    }

    public final DoubleProperty layoutXProperty() {
        if (layoutX == null) {
            layoutX = new DoublePropertyBase(0.0) {

                @Override
                protected void invalidated() {
                    NodeHelper.transformsChanged(Node.this);
                    final Parent p = getParent();

                    // Propagate layout if this change isn't triggered by its parent
                    if (p != null && !p.isCurrentLayoutChild(Node.this)) {
                        if (isManaged()) {
                            // Force its parent to fix the layout since it is a managed child.
                            p.requestLayout(true);
                        } else {
                            // Parent size changed, parent's parent might need to re-layout
                            p.clearSizeCache();
                            p.requestParentLayout();
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "layoutX";
                }
            };
        }
        return layoutX;
    }

    /**
     * Defines the y coordinate of the translation that is added to this {@code Node}'s
     * transform for the purpose of layout. The value should be computed as the
     * offset required to adjust the position of the node from its current
     * {@link #layoutBoundsProperty() layoutBounds minY} position (which might not be 0) to the desired location.
     *
     * <p>For example, if {@code textnode} should be positioned at {@code finalY}
     * <pre>{@code
     *     textnode.setLayoutY(finalY - textnode.getLayoutBounds().getMinY());
     * }</pre>
     * <p>
     * Failure to subtract {@code layoutBounds minY} may result in misplacement
     * of the node.  The {@link #relocate(double, double) relocate(x, y)} method will automatically do the
     * correct computation and should generally be used over setting layoutY directly.
     * <p>
     * The node's final translation will be computed as {@code layoutY} + {@link #translateYProperty translateY},
     * where {@code layoutY} establishes the node's stable position
     * and {@code translateY} optionally makes dynamic adjustments to that
     * position.
     * <p>
     * If the node is managed and has a {@link javafx.scene.layout.Region}
     * as its parent, then the region will set {@code layoutY} according to its
     * own layout policy.   If the node is unmanaged or parented by a {@link Group},
     * then the application may set {@code layoutY} directly to position it.
     *
     * @see #relocate(double, double)
     * @see #layoutBoundsProperty()
     */
    private DoubleProperty layoutY;

    public final void setLayoutY(double value) {
        layoutYProperty().set(value);
    }

    public final double getLayoutY() {
        return layoutY == null ? 0.0 : layoutY.get();
    }

    public final DoubleProperty layoutYProperty() {
        if (layoutY == null) {
            layoutY = new DoublePropertyBase(0.0) {

                @Override
                protected void invalidated() {
                    NodeHelper.transformsChanged(Node.this);
                    final Parent p = getParent();

                    // Propagate layout if this change isn't triggered by its parent
                    if (p != null && !p.isCurrentLayoutChild(Node.this)) {
                        if (isManaged()) {
                            // Force its parent to fix the layout since it is a managed child.
                            p.requestLayout(true);
                        } else {
                            // Parent size changed, parent's parent might need to re-layout
                            p.clearSizeCache();
                            p.requestParentLayout();
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "layoutY";
                }

            };
        }
        return layoutY;
    }

    /**
     * Sets the node's layoutX and layoutY translation properties in order to
     * relocate this node to the x,y location in the parent.
     * <p>
     * This method does not alter translateX or translateY, which if also set
     * will be added to layoutX and layoutY, adjusting the final location by
     * corresponding amounts.
     *
     * @param x the target x coordinate location
     * @param y the target y coordinate location
     */
    public void relocate(double x, double y) {
        setLayoutX(x - getLayoutBounds().getMinX());
        setLayoutY(y - getLayoutBounds().getMinY());

        PlatformLogger logger = Logging.getLayoutLogger();
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString()+" moved to ("+x+","+y+")");
        }
    }

    /**
     * Indicates whether this node is a type which can be resized by its parent.
     * If this method returns true, then the parent will resize the node (ideally
     * within its size range) by calling node.resize(width,height) during the
     * layout pass.  All Regions, Controls, and WebView are resizable classes
     * which depend on their parents resizing them during layout once all sizing
     * and CSS styling information has been applied.
     * <p>
     * If this method returns false, then the parent cannot resize it during
     * layout (resize() is a no-op) and it should return its layoutBounds for
     * minimum, preferred, and maximum sizes.  Group, Text, and all Shapes are not
     * resizable and hence depend on the application to establish their sizing
     * by setting appropriate properties (e.g.  width/height for Rectangle,
     * text on Text, and so on).  Non-resizable nodes may still be relocated
     * during layout.
     *
     * @see #getContentBias()
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #prefWidth(double)
     * @see #prefHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     * @see #resize(double, double)
     * @see #getLayoutBounds()
     *
     * @return whether or not this node type can be resized by its parent during layout
     */
    public boolean isResizable() {
        return false;
    }

    /**
     * Returns the orientation of a node's resizing bias for layout purposes.
     * If the node type has no bias, returns null.  If the node is resizable and
     * it's height depends on its width, returns HORIZONTAL, else if its width
     * depends on its height, returns VERTICAL.
     * <p>
     * Resizable subclasses should override this method to return an
     * appropriate value.
     *
     * @see #isResizable()
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #prefWidth(double)
     * @see #prefHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     *
     * @return orientation of width/height dependency or null if there is none
     */
    public Orientation getContentBias() {
        return null;
    }

    /**
     * Returns the node's minimum width for use in layout calculations.
     * If the node is resizable, its parent should not resize its width any
     * smaller than this value.  If the node is not resizable, returns its
     * layoutBounds width.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a vertical content-bias, then callers
     * should pass in a height value that the minimum width should be based on.
     * If the node has either a horizontal or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a vertical content-bias should honor the height
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the height parameter (which will likely be -1).
     * <p>
     * If Node's {@link #maxWidth(double)} is lower than this number,
     * {@code minWidth} takes precedence. This means the Node should never be resized below {@code minWidth}.
     *
     * @see #isResizable()
     * @see #getContentBias()
     *
     * @param height the height that should be used if minimum width depends on it
     * @return the minimum width that the node should be resized to during layout.
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double minWidth(double height) {
        return prefWidth(height);
    }

    /**
     * Returns the node's minimum height for use in layout calculations.
     * If the node is resizable, its parent should not resize its height any
     * smaller than this value.  If the node is not resizable, returns its
     * layoutBounds height.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a horizontal content-bias, then callers
     * should pass in a width value that the minimum height should be based on.
     * If the node has either a vertical or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a horizontal content-bias should honor the width
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the width parameter (which will likely be -1).
     * <p>
     * If Node's {@link #maxHeight(double)} is lower than this number,
     * {@code minHeight} takes precedence. This means the Node should never be resized below {@code minHeight}.
     *
     * @see #isResizable()
     * @see #getContentBias()
     *
     * @param width the width that should be used if minimum height depends on it
     * @return the minimum height that the node should be resized to during layout
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double minHeight(double width) {
        return prefHeight(width);
    }

    /**
     * Returns the node's preferred width for use in layout calculations.
     * If the node is resizable, its parent should treat this value as the
     * node's ideal width within its range.  If the node is not resizable,
     * just returns its layoutBounds width, which should be treated as the rigid
     * width of the node.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a vertical content-bias, then callers
     * should pass in a height value that the preferred width should be based on.
     * If the node has either a horizontal or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a vertical content-bias should honor the height
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the height parameter (which will likely be -1).
     *
     * @see #isResizable()
     * @see #getContentBias()
     * @see #autosize()
     *
     * @param height the height that should be used if preferred width depends on it
     * @return the preferred width that the node should be resized to during layout
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double prefWidth(double height) {
        final double result = getLayoutBounds().getWidth();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Returns the node's preferred height for use in layout calculations.
     * If the node is resizable, its parent should treat this value as the
     * node's ideal height within its range.  If the node is not resizable,
     * just returns its layoutBounds height, which should be treated as the rigid
     * height of the node.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a horizontal content-bias, then callers
     * should pass in a width value that the preferred height should be based on.
     * If the node has either a vertical or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a horizontal content-bias should honor the height
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the height parameter (which will likely be -1).
     *
     * @see #getContentBias()
     * @see #autosize()
     *
     * @param width the width that should be used if preferred height depends on it
     * @return the preferred height that the node should be resized to during layout
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double prefHeight(double width) {
        final double result = getLayoutBounds().getHeight();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Returns the node's maximum width for use in layout calculations.
     * If the node is resizable, its parent should not resize its width any
     * larger than this value.  A value of Double.MAX_VALUE indicates the
     * parent may expand the node's width beyond its preferred without limits.
     * <p>
     * If the node is not resizable, returns its layoutBounds width.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a vertical content-bias, then callers
     * should pass in a height value that the maximum width should be based on.
     * If the node has either a horizontal or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a vertical content-bias should honor the height
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the height parameter (which will likely be -1).
     * <p>
     * If Node's {@link #minWidth(double)} is greater, it should take precedence
     * over the {@code maxWidth}. This means the Node should never be resized below {@code minWidth}.
     *
     * @see #isResizable()
     * @see #getContentBias()
     *
     * @param height the height that should be used if maximum width depends on it
     * @return the maximum width that the node should be resized to during layout
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double maxWidth(double height) {
        return prefWidth(height);
    }

    /**
     * Returns the node's maximum height for use in layout calculations.
     * If the node is resizable, its parent should not resize its height any
     * larger than this value.  A value of Double.MAX_VALUE indicates the
     * parent may expand the node's height beyond its preferred without limits.
     * <p>
     * If the node is not resizable, returns its layoutBounds height.
     * <p>
     * Layout code which calls this method should first check the content-bias
     * of the node.  If the node has a horizontal content-bias, then callers
     * should pass in a width value that the maximum height should be based on.
     * If the node has either a vertical or null content-bias, then the caller
     * should pass in -1.
     * <p>
     * Node subclasses with a horizontal content-bias should honor the width
     * parameter whether -1 or a positive value.   All other subclasses may ignore
     * the width parameter (which will likely be -1).
     * <p>
     * If Node's {@link #minHeight(double)} is greater, it should take precedence
     * over the {@code maxHeight}.  This means the Node should never be resized below {@code minHeight}.
     *
     * @see #isResizable()
     * @see #getContentBias()
     *
     * @param width the width that should be used if maximum height depends on it
     * @return the maximum height that the node should be resized to during layout
     *         The result will never be NaN, nor will it ever be negative.
     */
    public double maxHeight(double width) {
        return prefHeight(width);
    }

    /**
     * If the node is resizable, will set its layout bounds to the specified
     * width and height.   If the node is not resizable, this method is a no-op.
     * <p>
     * This method should generally only be called by parent nodes from their
     * layoutChildren() methods.   All Parent classes will automatically resize
     * resizable children, so resizing done directly by the application will be
     * overridden by the node's parent, unless the child is unmanaged.
     * <p>
     * Parents are responsible for ensuring the width and height values fall
     * within the resizable node's preferred range.  The autosize() method may
     * be used if the parent just needs to resize the node to its preferred size.
     *
     * @see #isResizable()
     * @see #getContentBias()
     * @see #autosize()
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #prefWidth(double)
     * @see #prefHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     * @see #getLayoutBounds()
     *
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    public void resize(double width, double height) {
    }

    /**
     * If the node is resizable, will set its layout bounds to its current preferred
     * width and height. If the node is not resizable, this method is a no-op.
     * <p>
     * This method automatically queries the node's content-bias and if it's
     * horizontal, will pass in the node's preferred width to get the preferred
     * height; if vertical, will pass in the node's preferred height to get the width,
     * and if null, will compute the preferred width/height independently.
     * </p>
     *
     * @see #isResizable()
     * @see #getContentBias()
     *
     */
    public final void autosize() {
        if (isResizable()) {
            Orientation contentBias = getContentBias();
            double w, h;
            if (contentBias == null) {
                w = boundedSize(prefWidth(-1), minWidth(-1), maxWidth(-1));
                h = boundedSize(prefHeight(-1), minHeight(-1), maxHeight(-1));
            } else if (contentBias == Orientation.HORIZONTAL) {
                w = boundedSize(prefWidth(-1), minWidth(-1), maxWidth(-1));
                h = boundedSize(prefHeight(w), minHeight(w), maxHeight(w));
            } else { // bias == VERTICAL
                h = boundedSize(prefHeight(-1), minHeight(-1), maxHeight(-1));
                w = boundedSize(prefWidth(h), minWidth(h), maxWidth(h));
            }
            resize(w,h);
        }
    }

    double boundedSize(double value, double min, double max) {
        // if max < value, return max
        // if min > value, return min
        // if min > max, return min
        return Math.min(Math.max(value, min), Math.max(min,max));
    }

    /**
     * If the node is resizable, will set its layout bounds to the specified
     * width and height.   If the node is not resizable, the resize step is skipped.
     * <p>
     * Once the node has been resized (if resizable) then sets the node's layoutX
     * and layoutY translation properties in order to relocate it to x,y in the
     * parent's coordinate space.
     * <p>
     * This method should generally only be called by parent nodes from their
     * layoutChildren() methods.   All Parent classes will automatically resize
     * resizable children, so resizing done directly by the application will be
     * overridden by the node's parent, unless the child is unmanaged.
     * <p>
     * Parents are responsible for ensuring the width and height values fall
     * within the resizable node's preferred range.  The autosize() and relocate()
     * methods may be used if the parent just needs to resize the node to its
     * preferred size and reposition it.
     *
     * @see #isResizable()
     * @see #getContentBias()
     * @see #autosize()
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #prefWidth(double)
     * @see #prefHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     *
     * @param x the target x coordinate location
     * @param y the target y coordinate location
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     *
     */
    public void resizeRelocate(double x, double y, double width, double height) {
        resize(width, height);
        relocate(x,y);
    }

    /**
     * This is a special value that might be returned by {@link #getBaselineOffset()}.
     * This means that the Parent (layout Pane) of this Node should use the height of this Node as a baseline.
     */
    public static final double BASELINE_OFFSET_SAME_AS_HEIGHT = Double.NEGATIVE_INFINITY;

    /**
     * The 'alphabetic' (or 'roman') baseline offset from the node's layoutBounds.minY location
     * that should be used when this node is being vertically aligned by baseline with
     * other nodes.  By default this returns {@link #BASELINE_OFFSET_SAME_AS_HEIGHT} for resizable Nodes
     * and layoutBounds height for non-resizable.  Subclasses
     * which contain text should override this method to return their actual text baseline offset.
     *
     * @return offset of text baseline from layoutBounds.minY for non-resizable Nodes or {@link #BASELINE_OFFSET_SAME_AS_HEIGHT} otherwise
     */
    public double getBaselineOffset() {
        if (isResizable()) {
            return BASELINE_OFFSET_SAME_AS_HEIGHT;
        } else {
            return getLayoutBounds().getHeight();
        }
    }

    /**
     * Returns the area of this {@code Node} projected onto the
     * physical screen in pixel units.
     * @return the area of this {@code Node} projected onto the physical screen
     * @since JavaFX 8.0
     */
    public double computeAreaInScreen() {
        return doComputeAreaInScreen();
    }

    /*
     * Help application or utility to implement LOD support by returning the
     * projected area of a Node in pixel unit. The projected area is not clipped.
     *
     * For perspective camera, this method first exams node's bounds against
     * camera's clipping plane to cut off those out of viewing frustrum. After
     * computing areaInScreen, it applies a tight viewing frustrum check using
     * canonical view volume.
     *
     * The result of areaInScreen comes from the product of
     * (projViewTx x localToSceneTransform x localBounds).
     *
     * Returns 0 for those fall outside viewing frustrum.
     */
    private double doComputeAreaInScreen() {
        Scene tmpScene = getScene();
        if (tmpScene != null) {
            Bounds bounds = getBoundsInLocal();
            Camera camera = tmpScene.getEffectiveCamera();
            boolean isPerspective = camera instanceof PerspectiveCamera ? true : false;
            Transform localToSceneTx = getLocalToSceneTransform();
            Affine3D tempTx = TempState.getInstance().tempTx;
            BaseBounds localBounds = new BoxBounds((float) bounds.getMinX(),
                                                   (float) bounds.getMinY(),
                                                   (float) bounds.getMinZ(),
                                                   (float) bounds.getMaxX(),
                                                   (float) bounds.getMaxY(),
                                                   (float) bounds.getMaxZ());

            // NOTE: Viewing frustrum check on camera's clipping plane is now only
            // for perspective camera.
            // TODO: Need to hook up parallel camera's nearClip and farClip.
            if (isPerspective) {
                Transform cameraL2STx = camera.getLocalToSceneTransform();

                // If camera transform only contains translate, compare in scene
                // coordinate. Otherwise, compare in camera coordinate.
                if (cameraL2STx.getMxx() == 1.0
                        && cameraL2STx.getMxy() == 0.0
                        && cameraL2STx.getMxz() == 0.0
                        && cameraL2STx.getMyx() == 0.0
                        && cameraL2STx.getMyy() == 1.0
                        && cameraL2STx.getMyz() == 0.0
                        && cameraL2STx.getMzx() == 0.0
                        && cameraL2STx.getMzy() == 0.0
                        && cameraL2STx.getMzz() == 1.0) {

                    double minZ, maxZ;

                    // If node transform only contains translate, only convert
                    // minZ and maxZ to scene coordinate. Otherwise, convert
                    // node bounds to scene coordinate.
                    if (localToSceneTx.getMxx() == 1.0
                            && localToSceneTx.getMxy() == 0.0
                            && localToSceneTx.getMxz() == 0.0
                            && localToSceneTx.getMyx() == 0.0
                            && localToSceneTx.getMyy() == 1.0
                            && localToSceneTx.getMyz() == 0.0
                            && localToSceneTx.getMzx() == 0.0
                            && localToSceneTx.getMzy() == 0.0
                            && localToSceneTx.getMzz() == 1.0) {

                        Vec3d tempV3D = TempState.getInstance().vec3d;
                        tempV3D.set(0, 0, bounds.getMinZ());
                        localToScene(tempV3D);
                        minZ = tempV3D.z;

                        tempV3D.set(0, 0, bounds.getMaxZ());
                        localToScene(tempV3D);
                        maxZ = tempV3D.z;
                    } else {
                        Bounds nodeInSceneBounds = localToScene(bounds);
                        minZ = nodeInSceneBounds.getMinZ();
                        maxZ = nodeInSceneBounds.getMaxZ();
                    }

                    if (minZ > camera.getFarClipInScene()
                            || maxZ < camera.getNearClipInScene()) {
                        return 0;
                    }

                } else {
                    BaseBounds nodeInCameraBounds = new BoxBounds();

                    // We need to set tempTx to identity since it is a recycled transform.
                    // This is because TransformHelper.apply() is a matrix concatenation operation.
                    tempTx.setToIdentity();
                    TransformHelper.apply(localToSceneTx, tempTx);

                    // Convert node from local coordinate to camera coordinate
                    tempTx.preConcatenate(camera.getSceneToLocalTransform());
                    tempTx.transform(localBounds, nodeInCameraBounds);

                    // Compare in camera coordinate
                    if (nodeInCameraBounds.getMinZ() > camera.getFarClip()
                            || nodeInCameraBounds.getMaxZ() < camera.getNearClip()) {
                        return 0;
                    }
                }
            }

            GeneralTransform3D projViewTx = TempState.getInstance().projViewTx;
            projViewTx.set(camera.getProjViewTransform());

            // We need to set tempTx to identity since it is a recycled transform.
            // This is because TransformHelper.apply() is a matrix concatenation operation.
            tempTx.setToIdentity();
            TransformHelper.apply(localToSceneTx, tempTx);

            // The product of projViewTx * localToSceneTransform
            GeneralTransform3D tx = projViewTx.mul(tempTx);

            // Transform localBounds to projected bounds
            localBounds = tx.transform(localBounds, localBounds);
            double area = localBounds.getWidth() * localBounds.getHeight();

            // Use canonical view volume to check whether object is outside the
            // viewing frustrum
            if (isPerspective) {
                localBounds.intersectWith(-1, -1, 0, 1, 1, 1);
                area = (localBounds.getWidth() < 0 || localBounds.getHeight() < 0) ? 0 : area;
            }
            return area * (camera.getViewWidth() / 2 * camera.getViewHeight() / 2);
        }
        return 0;
    }

    /* *************************************************************************
     *                                                                         *
     * Bounds related APIs                                                     *
     *                                                                         *
     **************************************************************************/

    public final Bounds getBoundsInParent() {
        return boundsInParentProperty().get();
    }

    /**
     * The rectangular bounds of this {@code Node} in the parent coordinate system.
     * {@code boundsInParent} is calculated by taking the {@linkplain #boundsInLocalProperty local bounds} and applying
     * the node transforms as specified in the <a href="#Transformations">Transformations</a> section of the class doc.
     * <p>
     * The resulting bounds will be conceptually in the coordinate space of the
     * {@code Node}'s parent, however, the node need not have a parent to calculate
     * these bounds.
     * <p>
     * Note that this method does not take the node's visibility into account;
     * the computation is based on the geometry of this {@code Node} only.
     * <p>
     * This property will always have a non-null value.
     * <p>
     * Note that {@code boundsInParent} is automatically recomputed whenever the
     * geometry of a node changes, or when any of the following the change:
     * transforms {@code ObservableList}, any of the translate, layout or scale
     * variables, or the rotate variable. For this reason, it is an error
     * to bind any of these values in a node to an expression that depends upon
     * this variable. For example, the x or y variables of a shape, or
     * {@code translateX}, {@code translateY} should never be bound to
     * {@code boundsInParent} for the purpose of positioning the node.
     * <p>
     * See also the <a href="#BoundingRectangles">Bounding Rectangles</a> section.
     *
     * @return the {@code boundsInParent} property for this {@code Node}
     */
    public final ReadOnlyObjectProperty<Bounds> boundsInParentProperty() {
        return getMiscProperties().boundsInParentProperty();
    }

    private void invalidateBoundsInParent() {
        if (miscProperties != null) {
            miscProperties.invalidateBoundsInParent();
        }
    }

    public final Bounds getBoundsInLocal() {
        return boundsInLocalProperty().get();
    }

    /**
     * The rectangular bounds of this {@code Node} in the node's
     * untransformed local coordinate space.  For nodes that extend
     * {@link javafx.scene.shape.Shape}, the local bounds will also include
     * space required for a non-zero stroke that may fall outside the shape's
     * geometry that is defined by position and size attributes.
     * The local bounds will also include any clipping set with {@link #clipProperty clip}
     * as well as effects set with {@link #effectProperty effect}.
     *
     * <p>
     * Note that this method does not take the node's visibility into account;
     * the computation is based on the geometry of this {@code Node} only.
     * <p>
     * This property will always have a non-null value.
     * <p>
     * Note that boundsInLocal is automatically recomputed whenever the
     * geometry of a node changes. For this reason, it is an error to bind any
     * of these values in a node to an expression that depends upon this variable.
     * For example, the x or y variables of a shape should never be bound
     * to boundsInLocal for the purpose of positioning the node.
     * @return the boundsInLocal for this {@code Node}
     */
    public final ReadOnlyObjectProperty<Bounds> boundsInLocalProperty() {
        return getMiscProperties().boundsInLocalProperty();
    }

    private void invalidateBoundsInLocal() {
        if (miscProperties != null) {
            miscProperties.invalidateBoundsInLocal();
        }
    }

    /**
     * The rectangular bounds that should be used for layout calculations for
     * this node. {@code layoutBounds} may differ from the visual bounds
     * of the node and is computed differently depending on the node type.
     * <p>
     * If the node type is resizable ({@link javafx.scene.layout.Region Region},
     * {@link javafx.scene.control.Control Control}, or {@link javafx.scene.web.WebView WebView})
     * then the layoutBounds will always be {@code 0,0 width x height}.
     * If the node type is not resizable ({@link javafx.scene.shape.Shape Shape},
     * {@link javafx.scene.text.Text Text}, or {@link Group}), then the {@code layoutBounds}
     * are computed based on the node's geometric properties and does not include the
     * node's clip, effect, or transforms.  See individual class documentation
     * for details.
     * <p>
     * Note that the {@link #layoutXProperty layoutX}, {@link #layoutYProperty layoutY},
     * {@link #translateXProperty translateX}, and {@link #translateYProperty translateY}
     * variables are not included in the layoutBounds.
     * This is important because layout code must first determine the current
     * size and location of the node (using {@code layoutBounds}) and then set
     * {@code layoutX} and {@code layoutY} to adjust the translation of the
     * node so that it will have the desired layout position.
     * <p>
     * Because the computation of layoutBounds is often tied to a node's
     * geometric variables, it is an error to bind any such variables to an
     * expression that depends upon {@code layoutBounds}. For example, the
     * x or y variables of a shape should never be bound to {@code layoutBounds}
     * for the purpose of positioning the node.
     * <p>
     * Note that for 3D shapes, the layout bounds is actually a rectangular box
     * with X, Y, and Z values, although only X and Y are used in layout calculations.
     * <p>
     * The {@code layoutBounds} will never be null.
     *
     */
    private LazyBoundsProperty layoutBounds = new LazyBoundsProperty() {
        @Override
        protected Bounds computeBounds() {
            return NodeHelper.computeLayoutBounds(Node.this);
        }

        @Override
        public Object getBean() {
            return Node.this;
        }

        @Override
        public String getName() {
            return "layoutBounds";
        }
    };

    public final Bounds getLayoutBounds() {
        return layoutBoundsProperty().get();
    }

    public final ReadOnlyObjectProperty<Bounds> layoutBoundsProperty() {
        return layoutBounds;
    }

    /*
     *                  Bounds And Transforms Computation
     *
     *  This section of the code is responsible for computing and caching
     *  various bounds and transforms. For optimal performance and minimal
     *  recomputation of bounds (which can be quite expensive), we cache
     *  values on two different levels. We expose two public immutable
     *  Bounds boundsInParent objects and boundsInLocal. Because they are
     *  immutable and because they may change quite frequently (especially
     *  in the case of a Parent whose children are animated), it is
     *  important that the system does not rely on these variables, because
     *  doing so would produce a large amount of garbage. Rather, these
     *  variables are provided solely for the convenience of application
     *  developers and, being lazily bound, should generally be created at
     *  most once per frame.
     *
     *  The second level of caching are within local Bounds2D variables.
     *  These variables, txBounds and geomBounds, are mutable and as such
     *  can be cached and updated as frequently as necessary without creating
     *  excessive garbage. However, since the computation of bounds is still
     *  expensive, it is desirable to cache both the geometric bounds and
     *  the "complete" transformed bounds (essentially, boundsInParent).
     *  Cached txBounds is particularly useful when computing the geometric
     *  bounds of a Parent since it would not require complete or partial
     *  recomputation of each child.
     *
     *  Finally, we cache the complete transform for this node which converts
     *  its coord system from local to parent coords. This is useful both for
     *  minimizing bounds recomputations in the case of the geometry having
     *  changed but the transform not having changed, and also because the tx
     *  is required for several different computations (for example, it must
     *  be computed once during state synchronization with the PG peer, and
     *  must also be computed when the pivot point changes, and also when
     *  deriving the txBounds of the Node).
     *
     *  As with any caching system, a subtle and non-trivial amount of code
     *  is devoted to invalidating the bounds / transforms at appropriate
     *  times and in appropriate places to make sure bounds / transforms
     *  are recomputed at all necessary times.
     *
     *  There are three computeXXX functions. One is for computing the
     *  boundsInParent, the second for computing boundsInLocal, and the
     *  third for computing the default layout bounds (which, by default,
     *  is based on the geometric bounds). These functions are all prefixed
     *  with "compute" because they create and return new immutable
     *  Bounds objects.
     *
     *  There are three getXXXBounds functions. One is for returning the
     *  complete transformed bounds. The second is for returning the
     *  local bounds. The last is for returning the geometric bounds. These
     *  functions are all prefixed with "get" because they may well return
     *  a cached value, or may actually compute the bounds if necessary. These
     *  functions all have the same signature. They take a Bounds2D and
     *  BaseTransform, and return a Bounds2D (the same as they took). These
     *  functions essentially populate the supplied bounds2D with the
     *  appropriate bounds information, leveraging cached bounds if possible.
     *
     *  There is a single NodeHelper.computeGeomBoundsImpl function which is abstract.
     *  This must be implemented in each subclass, and is responsible for
     *  computing the actual geometric bounds for the Node. For example, Parent
     *  is written such that this function is the union of the transformed
     *  bounds of each child. Rectangle is written such that this takes into
     *  account the size and stroke. Text is written such that it is computed
     *  based on the actual glyphs.
     *
     *  There are two updateXXX functions, updateGeomBounds and updateTxBounds.
     *  These functions are for ensuring that geomBounds and txBounds are
     *  valid. They only execute in the case of the cached value being invalid,
     *  so the function call is very cheap in cases where the cached bounds
     *  values are still valid.
     */

    /**
     * An affine transform that holds the computed local-to-parent transform.
     * This is the concatenation of all transforms in this node, including all
     * of the convenience transforms.
     */
    private BaseTransform localToParentTx = BaseTransform.IDENTITY_TRANSFORM;

    /**
     * This flag is used to indicate that localToParentTx is dirty and needs
     * to be recomputed.
     */
    private boolean transformDirty = true;

    /**
     * The cached transformed bounds. This is never null, but is frequently set
     * to be invalid whenever the bounds for the node have changed. These are
     * "complete" bounds, that is, with transforms and effect and clip applied.
     * Note that this is equivalent to boundsInParent
     */
    private BaseBounds txBounds = new RectBounds();

    /**
     * The cached bounds. This is never null, but is frequently set to be
     * invalid whenever the bounds for the node have changed. These are the
     * "content" bounds, that is, without transforms or effects applied.
     */
    private BaseBounds geomBounds = new RectBounds();

    /**
     * The cached local bounds (without transforms, with clip and effects).
     * If there is neither clip nor effect
     * local bounds are equal to geom bounds, so in this case we don't keep
     * the extra instance and set null to this variable.
     */
    private BaseBounds localBounds = null;

    /**
     * This special flag is used only by Parent to flag whether or not
     * the *parent* has processed the fact that bounds have changed for this
     * child Node. We need some way of flagging this on a per-node basis to
     * enable the significant performance optimizations and fast paths that
     * are in the Parent code.
     * <p>
     * To reduce confusion, although this variable is defined on Node, it
     * really belongs to the Parent of the node and should *only* be modified
     * by the parent.
     */
    boolean boundsChanged;

    /*
     * Returns geometric bounds, but may be over-ridden by a subclass.
     */
    private Bounds doComputeLayoutBounds() {
        BaseBounds tempBounds = TempState.getInstance().bounds;
        tempBounds = getGeomBounds(tempBounds,
                                   BaseTransform.IDENTITY_TRANSFORM);
        return new BoundingBox(tempBounds.getMinX(),
                               tempBounds.getMinY(),
                               tempBounds.getMinZ(),
                               tempBounds.getWidth(),
                               tempBounds.getHeight(),
                               tempBounds.getDepth());
    }

    /*
     * Subclasses may customize the layoutBounds by means of overriding the
     * NodeHelper.computeLayoutBoundsImpl method. If the layout bounds need to be
     * recomputed, the subclass must notify the Node implementation of this
     * fact so that appropriate notifications and internal state can be
     * kept in sync. Subclasses must call NodeHelper.layoutBoundsChanged to
     * let Node know that the layout bounds are invalid and need to be
     * recomputed.
     */
    final void layoutBoundsChanged() {
        if (!layoutBounds.valid) {
            return;
        }
        layoutBounds.invalidate();
        if ((nodeTransformation != null && nodeTransformation.hasScaleOrRotate()) || hasMirroring()) {
            // if either the scale or rotate convenience variables are used,
            // then we need a valid pivot point. Since the layoutBounds
            // affects the pivot we need to invalidate the transform
            NodeHelper.transformsChanged(this);
        }
    }

    /**
     * Loads the given bounds object with the transformed bounds relative to,
     * and based on, the given transform. That is, this is the local bounds
     * with the local-to-parent transform applied.
     *
     * We *never* pass null in as a bounds. This method will
     * NOT take a null bounds object. The returned value may be
     * the same bounds object passed in, or it may be a new object.
     * The reason for this object promotion is in the case of needing
     * to promote from a RectBounds to a BoxBounds (3D).
     */
    BaseBounds getTransformedBounds(BaseBounds bounds, BaseTransform tx) {
        updateLocalToParentTransform();
        if (tx.isTranslateOrIdentity()) {
            updateTxBounds();
            bounds = bounds.deriveWithNewBounds(txBounds);
            if (!tx.isIdentity()) {
                final double translateX = tx.getMxt();
                final double translateY = tx.getMyt();
                final double translateZ = tx.getMzt();
                bounds = bounds.deriveWithNewBounds(
                                    (float) (bounds.getMinX() + translateX),
                                    (float) (bounds.getMinY() + translateY),
                                    (float) (bounds.getMinZ() + translateZ),
                                    (float) (bounds.getMaxX() + translateX),
                                    (float) (bounds.getMaxY() + translateY),
                                    (float) (bounds.getMaxZ() + translateZ));
            }
            return bounds;
        } else if (localToParentTx.isIdentity()) {
            return getLocalBounds(bounds, tx);
        } else {
            double mxx = tx.getMxx();
            double mxy = tx.getMxy();
            double mxz = tx.getMxz();
            double mxt = tx.getMxt();
            double myx = tx.getMyx();
            double myy = tx.getMyy();
            double myz = tx.getMyz();
            double myt = tx.getMyt();
            double mzx = tx.getMzx();
            double mzy = tx.getMzy();
            double mzz = tx.getMzz();
            double mzt = tx.getMzt();
            BaseTransform boundsTx = tx.deriveWithConcatenation(localToParentTx);
            bounds = getLocalBounds(bounds, boundsTx);
            if (boundsTx == tx) {
                tx.restoreTransform(mxx, mxy, mxz, mxt,
                                    myx, myy, myz, myt,
                                    mzx, mzy, mzz, mzt);
            }
            return bounds;
        }
    }

    /**
     * Loads the given bounds object with the local bounds relative to,
     * and based on, the given transform. That is, these are the geometric
     * bounds + clip and effect.
     *
     * We *never* pass null in as a bounds. This method will
     * NOT take a null bounds object. The returned value may be
     * the same bounds object passed in, or it may be a new object.
     * The reason for this object promotion is in the case of needing
     * to promote from a RectBounds to a BoxBounds (3D).
     */
    BaseBounds getLocalBounds(BaseBounds bounds, BaseTransform tx) {
        if (getEffect() == null && getClip() == null) {
            return getGeomBounds(bounds, tx);
        }

        if (tx.isTranslateOrIdentity()) {
            // we can take a fast path since we know tx is either a simple
            // translation or is identity
            updateLocalBounds();
            bounds = bounds.deriveWithNewBounds(localBounds);
            if (!tx.isIdentity()) {
                double translateX = tx.getMxt();
                double translateY = tx.getMyt();
                double translateZ = tx.getMzt();
                bounds = bounds.deriveWithNewBounds((float) (bounds.getMinX() + translateX),
                        (float) (bounds.getMinY() + translateY),
                        (float) (bounds.getMinZ() + translateZ),
                        (float) (bounds.getMaxX() + translateX),
                        (float) (bounds.getMaxY() + translateY),
                        (float) (bounds.getMaxZ() + translateZ));
            }
            return bounds;
        } else if (tx.is2D()
                && (tx.getType()
                & ~(BaseTransform.TYPE_UNIFORM_SCALE | BaseTransform.TYPE_TRANSLATION
                | BaseTransform.TYPE_FLIP | BaseTransform.TYPE_QUADRANT_ROTATION)) != 0) {
            // this is a non-uniform scale / non-quadrant rotate / skew transform
            return computeLocalBounds(bounds, tx);
        } else {
            // 3D transformations and
            // selected 2D transformations (uniform transform, flip, quadrant rotation).
            // These 2D transformation will yield tight bounds when applied on the pre-computed
            // geomBounds
            // Note: Transforming the local bounds into a 3D space will yield a bounds
            // that isn't as tight as transforming its geometry and compute it bounds.
            updateLocalBounds();
            return tx.transform(localBounds, bounds);
        }
    }

    /**
     * Loads the given bounds object with the geometric bounds relative to,
     * and based on, the given transform.
     *
     * We *never* pass null in as a bounds. This method will
     * NOT take a null bounds object. The returned value may be
     * the same bounds object passed in, or it may be a new object.
     * The reason for this object promotion is in the case of needing
     * to promote from a RectBounds to a BoxBounds (3D).
     */
    BaseBounds getGeomBounds(BaseBounds bounds, BaseTransform tx) {
        if (tx.isTranslateOrIdentity()) {
            // we can take a fast path since we know tx is either a simple
            // translation or is identity
            updateGeomBounds();
            bounds = bounds.deriveWithNewBounds(geomBounds);
            if (!tx.isIdentity()) {
                double translateX = tx.getMxt();
                double translateY = tx.getMyt();
                double translateZ = tx.getMzt();
                bounds = bounds.deriveWithNewBounds((float) (bounds.getMinX() + translateX),
                        (float) (bounds.getMinY() + translateY),
                        (float) (bounds.getMinZ() + translateZ),
                        (float) (bounds.getMaxX() + translateX),
                        (float) (bounds.getMaxY() + translateY),
                        (float) (bounds.getMaxZ() + translateZ));
            }
            return bounds;
        } else if (tx.is2D()
                && (tx.getType()
                & ~(BaseTransform.TYPE_UNIFORM_SCALE | BaseTransform.TYPE_TRANSLATION
                | BaseTransform.TYPE_FLIP | BaseTransform.TYPE_QUADRANT_ROTATION)) != 0) {
            // this is a non-uniform scale / non-quadrant rotate / skew transform
            return NodeHelper.computeGeomBounds(this, bounds, tx);
        } else {
            // 3D transformations and
            // selected 2D transformations (unifrom transform, flip, quadrant rotation).
            // These 2D transformation will yield tight bounds when applied on the pre-computed
            // geomBounds
            // Note: Transforming the local geomBounds into a 3D space will yield a bounds
            // that isn't as tight as transforming its geometry and compute it bounds.
            updateGeomBounds();
            return tx.transform(geomBounds, bounds);
        }
    }

    /**
     * If necessary, recomputes the cached geom bounds. If the bounds are not
     * invalid, then this method is a no-op.
     */
    void updateGeomBounds() {
        if (geomBoundsInvalid) {
            geomBounds = NodeHelper.computeGeomBounds(this, geomBounds, BaseTransform.IDENTITY_TRANSFORM);
            geomBoundsInvalid = false;
        }
    }

    /**
     * Computes the local bounds of this Node.
     */
    private BaseBounds computeLocalBounds(BaseBounds bounds, BaseTransform tx) {
        // We either get the bounds of the effect (if it isn't null)
        // or we get the geom bounds (if effect is null). We will then
        // intersect this with the clip.
        if (getEffect() != null) {
            BaseBounds b = EffectHelper.getBounds(getEffect(), bounds, tx, this, boundsAccessor);
            bounds = bounds.deriveWithNewBounds(b);
        } else {
            bounds = getGeomBounds(bounds, tx);
        }
        // intersect with the clip. Take care with "bounds" as it may
        // actually be TEMP_BOUNDS, so we save off state
        if (getClip() != null
                // FIXME: All 3D picking is currently ignored by rendering.
                // Until this is fixed or defined differently (JDK-8090485),
                // we follow this behavior.
                && !(this instanceof Shape3D) && !(getClip() instanceof Shape3D)) {
            double x1 = bounds.getMinX();
            double y1 = bounds.getMinY();
            double x2 = bounds.getMaxX();
            double y2 = bounds.getMaxY();
            double z1 = bounds.getMinZ();
            double z2 = bounds.getMaxZ();
            bounds = getClip().getTransformedBounds(bounds, tx);
            bounds.intersectWith((float)x1, (float)y1, (float)z1,
                    (float)x2, (float)y2, (float)z2);
        }
        return bounds;
    }


    /**
     * If necessary, recomputes the cached local bounds. If the bounds are not
     * invalid, then this method is a no-op.
     */
    private void updateLocalBounds() {
        if (localBoundsInvalid) {
            if (getClip() != null || getEffect() != null) {
                localBounds = computeLocalBounds(
                        localBounds == null ? new RectBounds() : localBounds,
                        BaseTransform.IDENTITY_TRANSFORM);
            } else {
                localBounds = null;
            }
            localBoundsInvalid = false;
        }
    }

    /**
     * If necessary, recomputes the cached transformed bounds.
     * If the cached transformed bounds are not invalid, then
     * this method is a no-op.
     */
    void updateTxBounds() {
        if (txBoundsInvalid) {
            updateLocalToParentTransform();
            txBounds = getLocalBounds(txBounds, localToParentTx);
            txBoundsInvalid = false;
        }
    }

    /*
     *                   Bounds Invalidation And Notification
     *
     *  The goal of this section is to efficiently propagate bounds
     *  invalidation through the scenegraph while also being semantically
     *  correct.
     *
     *  The code path for invalidation of layout bounds is somewhat confusing
     *  primarily due to performance enhancements and the desire to reduce the
     *  number of requestLayout() calls that are performed when layout bounds
     *  change. Before diving into layout bounds, I will first describe how
     *  normal bounds invalidation occurs.
     *
     *  When a node's geometry changes (for example, if the width of a
     *  Rectangle is changed) then the Node must call NodeHelper.geomChanged().
     *  Invoking this function will eventually clear all cached bounds and
     *  notify to each parent up the tree that their bounds may have changed.
     *
     *  After invalidating geomBounds (and after kicking off layout bounds
     *  notification), NodeHelper.geomChanged calls localBoundsChanged(). It should
     *  be noted that NodeHelper.geomChanged should only be called when the geometry
     *  of the node has changed such that it may result in the geom bounds
     *  actually changing.
     *
     *  localBoundsChanged() simply invalidates boundsInLocal and then calls
     *  transformedBoundsChanged().
     *
     *  transformedBoundsChanged() is responsible for invalidating
     *  boundsInParent and txBounds. If the Node is not visible, then there is
     *  no need to notify the parent of the bounds change because the parent's
     *  bounds do not include invisible nodes. If the node is visible, then
     *  it must tell the parent that this child node's bounds have changed.
     *  It is up to the parent to eventually invoke its own NodeHelper.geomChanged
     *  function. If instead of a parent this node has a clipParent, then the
     *  clipParent's localBoundsChanged() is called instead.
     *
     *  There are a few other ways in which we enter the invalidate steps
     *  beyond just the geometry changes. If the visibility of a Node changes,
     *  its own bounds are not affected but its parent's bounds are. So a
     *  special call to parent.childVisibilityChanged is made so the parent
     *  can react accordingly.
     *
     *  If a transform is changed (layoutX, layoutY, rotate, transforms, etc)
     *  then the transform must be invalidated. When a transform is invalidated,
     *  it must also invalidate the txBounds by invoking
     *  transformedBoundsChanged, which will in turn notify the parent as
     *  before.
     *
     *  If an effect is changed or replaced then the local bounds must be
     *  invalidated, as well as the transformedBounds and the parent notified
     *  of the change in bounds.
     *
     *  layoutBound is somewhat unique in that it can be redefined in
     *  subclasses. By default, the layoutBounds is the geomBounds, and so
     *  whenever the geomBounds() function is called the layoutBounds
     *  must be invalidated. However in subclasses, especially Resizables,
     *  the layout bounds may not be defined to be the same as the geometric
     *  bounds. This is both useful and provides a very nice performance
     *  optimization for regions and controls. In this case, subclasses
     *  need some way to interpose themselves such that a call to
     *  NodeHelper.geomChanged() *does not* invalidate the layout bounds.
     *
     *  This interposition happens by providing the
     *  NodeHelper.notifyLayoutBoundsChanged function. The default implementation
     *  simply invalidates boundsInLocal. Subclasses (such as Region and
     *  Control) can override this function so that it does not invalidate
     *  the layout bounds.
     *
     *  An on invalidate trigger on layoutBounds handles kicking off the rest
     *  of the invalidate process for layoutBounds. Because the layout bounds
     *  define the pivot point, if scaleX, scaleY, or rotate contain
     *  non-identity values then whenever the layoutBounds change the
     *  transformed bounds also change. Finally, if this node's parent is
     *  a Region and if the Node is being managed by the Region, then
     *  we must call requestLayout on the Region whenever the layout bounds
     *  have changed.
     */

    /*
     * Invoked by subclasses whenever their geometric bounds have changed.
     * Because the default layout bounds is based on the node geometry, this
     * function will invoke NodeHelper.notifyLayoutBoundsChanged. The default
     * implementation of NodeHelper.notifyLayoutBoundsChanged() will simply invalidate
     * layoutBounds. Resizable subclasses will want to override this function
     * in most cases to be a no-op.
     *
     * This function will also invalidate the cached geom bounds, and then
     * invoke localBoundsChanged() which will eventually end up invoking a
     * chain of functions up the tree to ensure that each parent of this
     * Node is notified that its bounds may have also changed.
     *
     * This function should be treated as though it were final. It is not
     * intended to be overridden by subclasses.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doGeomChanged() {
        if (geomBoundsInvalid) {
            // GeomBoundsInvalid is false when node geometry changed and
            // the untransformed node bounds haven't been recalculated yet.
            // Most of the time, the recalculation of layout and transformed
            // node bounds don't require validation of untransformed bounds
            // and so we can not skip the following notifications.
            NodeHelper.notifyLayoutBoundsChanged(this);
            transformedBoundsChanged();
            return;
        }
        geomBounds.makeEmpty();
        geomBoundsInvalid = true;
        NodeHelper.markDirty(this, DirtyBits.NODE_BOUNDS);
        NodeHelper.notifyLayoutBoundsChanged(this);
        localBoundsChanged();
    }

    private boolean geomBoundsInvalid = true;
    private boolean localBoundsInvalid = true;
    private boolean txBoundsInvalid = true;

    /**
     * Responds to changes in the local bounds by invalidating boundsInLocal
     * and notifying this node that its transformed bounds have changed.
     */
    void localBoundsChanged() {
        localBoundsInvalid = true;
        invalidateBoundsInLocal();
        transformedBoundsChanged();
    }

    /**
     * Responds to changes in the transformed bounds by invalidating txBounds
     * and boundsInParent. If this Node is not visible, then we have no need
     * to walk further up the tree but can instead simply invalidate state.
     * Otherwise, this function will notify parents (either the parent or the
     * clipParent) that this child Node's bounds have changed.
     */
    void transformedBoundsChanged() {
        if (!txBoundsInvalid) {
            txBounds.makeEmpty();
            txBoundsInvalid = true;
            invalidateBoundsInParent();
            NodeHelper.markDirty(this, DirtyBits.NODE_TRANSFORMED_BOUNDS);
        }
        if (isVisible()) {
            notifyParentOfBoundsChange();
        }
    }

    /*
     * Invoked by geomChanged(). Since layoutBounds is by default based
     * on the geometric bounds, the default implementation of this function will
     * invalidate the layoutBounds. Resizable Node subclasses generally base
     * layoutBounds on the width/height instead of the geometric bounds, and so
     * will generally want to override this function to be a no-op.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doNotifyLayoutBoundsChanged() {
        layoutBoundsChanged();
        // notify the parent
        // Group instanceof check a little hoaky, but it allows us to disable
        // unnecessary layout for the case of a non-resizable within a group
        Parent p = getParent();

        // Need to propagate layout if parent isn't part of performing layout
        if (isManaged() && (p != null) && !(p instanceof Group && !isResizable())
                && !p.isPerformingLayout()) {
            // Force its parent to fix the layout since it is a managed child.
            p.requestLayout(true);
        }
    }

    /**
     * Notifies both the real parent and the clip parent (if they exist) that
     * the bounds of the child has changed. Note that since FX doesn't throw
     * NPE's, things actually are faster if we don't check twice for Null
     * (we check once, the compiler checks again)
     */
    void notifyParentOfBoundsChange() {
        // let the parent know which node has changed and the parent will
        // deal with marking itself invalid correctly
        Parent p = getParent();
        if (p != null) {
            p.childBoundsChanged(this);
        }
        // since the clip is used to compute the local bounds (and not the
        // geom bounds), we just need to notify that local bounds on the
        // clip parent have changed
        if (clipParent != null) {
            clipParent.localBoundsChanged();
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Geometry and coordinate system related APIs. For example, methods       *
     * related to containment, intersection, coordinate space conversion, etc. *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns {@code true} if the given point (specified in the local
     * coordinate space of this {@code Node}) is contained within the shape of
     * this {@code Node}. Note that this method does not take visibility into
     * account; the test is based on the geometry of this {@code Node} only.
     * @param localX the x coordinate of the point in Node's space
     * @param localY the y coordinate of the point in Node's space
     * @return the result of contains for this {@code Node}
     */
    public boolean contains(double localX, double localY) {
        if (containsBounds(localX, localY)) {
            return (isPickOnBounds() || NodeHelper.computeContains(this, localX, localY));
        }
        return false;
    }

    /*
     * This method only does the contains check based on the bounds, clip and
     * effect of this node, excluding its shape (or geometry).
     *
     * Returns true if the given point (specified in the local
     * coordinate space of this {@code Node}) is contained within the bounds,
     * clip and effect of this node.
     */
    private boolean containsBounds(double localX, double localY) {
        final TempState tempState = TempState.getInstance();
        BaseBounds tempBounds = tempState.bounds;

        // first, we do a quick test to see if the point is contained in
        // our local bounds. If so, then we will go the next step and check
        // the clip, effect, and geometry for containment.
        tempBounds = getLocalBounds(tempBounds,
                                    BaseTransform.IDENTITY_TRANSFORM);
        if (tempBounds.contains((float)localX, (float)localY)) {
            // if the clip is defined, then check it for containment, being
            // sure to convert from this node's local coordinate system
            // to the local coordinate system of the clip node
            if (getClip() != null) {
                tempState.point.x = (float)localX;
                tempState.point.y = (float)localY;
                try {
                    getClip().parentToLocal(tempState.point);
                } catch (NoninvertibleTransformException e) {
                    return false;
                }
                if (!getClip().contains(tempState.point.x, tempState.point.y)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given point (specified in the local
     * coordinate space of this {@code Node}) is contained within the shape of
     * this {@code Node}. Note that this method does not take visibility into
     * account; the test is based on the geometry of this {@code Node} only.
     * @param localPoint the 2D point in Node's space
     * @return the result of contains for this {@code Node}
     */
    public boolean contains(Point2D localPoint) {
        return contains(localPoint.getX(), localPoint.getY());
    }

    /**
     * Returns {@code true} if the given rectangle (specified in the local
     * coordinate space of this {@code Node}) intersects the shape of this
     * {@code Node}. Note that this method does not take visibility into
     * account; the test is based on the geometry of this {@code Node} only.
     * The default behavior of this function is simply to check if the
     * given coordinates intersect with the local bounds.
     * @param localX the x coordinate of a rectangle in Node's space
     * @param localY the y coordinate of a rectangle in Node's space
     * @param localWidth the width of a rectangle in Node's space
     * @param localHeight the height of a rectangle in Node's space
     * @return the result of intersects for this {@code Node}
     */
    public boolean intersects(double localX, double localY, double localWidth, double localHeight) {
        BaseBounds tempBounds = TempState.getInstance().bounds;
        tempBounds = getLocalBounds(tempBounds,
                                    BaseTransform.IDENTITY_TRANSFORM);
        return tempBounds.intersects((float)localX,
                                     (float)localY,
                                     (float)localWidth,
                                     (float)localHeight);
    }

    /**
     * Returns {@code true} if the given bounds (specified in the local
     * coordinate space of this {@code Node}) intersects the shape of this
     * {@code Node}. Note that this method does not take visibility into
     * account; the test is based on the geometry of this {@code Node} only.
     * The default behavior of this function is simply to check if the
     * given coordinates intersect with the local bounds.
     * @param localBounds the bounds
     * @return the result of intersects for this {@code Node}
     */
    public boolean intersects(Bounds localBounds) {
        return intersects(localBounds.getMinX(), localBounds.getMinY(), localBounds.getWidth(), localBounds.getHeight());
    }

    /**
     * Transforms a point from the coordinate space of the {@link javafx.stage.Screen}
     * into the local coordinate space of this {@code Node}.
     * @param screenX x coordinate of a point on a Screen
     * @param screenY y coordinate of a point on a Screen
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     * @since JavaFX 8.0
     */
    public Point2D screenToLocal(double screenX, double screenY) {
        Scene scene = getScene();
        if (scene == null) return null;
        Window window = scene.getWindow();
        if (window == null) return null;

        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;

        tempPt.setLocation((float)(screenX - scene.getX() - window.getX()),
                           (float)(screenY - scene.getY() - window.getY()));

        final SubScene subScene = getSubScene();
        if (subScene != null) {
            final Point2D ssCoord = SceneUtils.sceneToSubScenePlane(subScene,
                    new Point2D(tempPt.x, tempPt.y));
            if (ssCoord == null) {
                return null;
            }
            tempPt.setLocation((float) ssCoord.getX(), (float) ssCoord.getY());
        }

        final Point3D ppIntersect =
                scene.getEffectiveCamera().pickProjectPlane(tempPt.x, tempPt.y);
        tempPt.setLocation((float) ppIntersect.getX(), (float) ppIntersect.getY());

        try {
            sceneToLocal(tempPt);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new Point2D(tempPt.x, tempPt.y);
    }

    /**
     * Transforms a point from the coordinate space of the {@link javafx.stage.Screen}
     * into the local coordinate space of this {@code Node}.
     * @param screenPoint a point on a Screen
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     * @since JavaFX 8.0
     */
    public Point2D screenToLocal(Point2D screenPoint) {
        return screenToLocal(screenPoint.getX(), screenPoint.getY());
    }

    /**
     * Transforms a rectangle from the coordinate space of the
     * {@link javafx.stage.Screen} into the local coordinate space of this
     * {@code Node}. Returns reasonable result only in 2D space.
     * @param screenBounds bounds on a Screen
     * @return bounds in the local Node'space or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     * @since JavaFX 8.0
     */
    public Bounds screenToLocal(Bounds screenBounds) {
        final Point2D p1 = screenToLocal(screenBounds.getMinX(), screenBounds.getMinY());
        final Point2D p2 = screenToLocal(screenBounds.getMinX(), screenBounds.getMaxY());
        final Point2D p3 = screenToLocal(screenBounds.getMaxX(), screenBounds.getMinY());
        final Point2D p4 = screenToLocal(screenBounds.getMaxX(), screenBounds.getMaxY());

        return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
    }


    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * arguments are in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #sceneToLocal(double, double)}.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return local coordinates of the point
     * @since JavaFX 8u40
     */
    public Point2D sceneToLocal(double x, double y, boolean rootScene) {
        if (!rootScene) {
            return sceneToLocal(x, y);
        }
        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;

        tempPt.setLocation((float)(x), (float)y);

        final SubScene subScene = getSubScene();
        if (subScene != null) {
            final Point2D ssCoord = SceneUtils.sceneToSubScenePlane(subScene,
                    new Point2D(tempPt.x, tempPt.y));
            if (ssCoord == null) {
                return null;
            }
            tempPt.setLocation((float) ssCoord.getX(), (float) ssCoord.getY());
        }

        try {
            sceneToLocal(tempPt);
            return new Point2D(tempPt.x, tempPt.y);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
    }

    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * arguments are in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #sceneToLocal(javafx.geometry.Point2D)}.
     *
     * @param point the point
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return local coordinates of the point
     * @since JavaFX 8u40
     */
    public Point2D sceneToLocal(Point2D point, boolean rootScene) {
        return sceneToLocal(point.getX(), point.getY(), rootScene);
    }

    /**
     * Transforms a bounds from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * arguments are in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #sceneToLocal(javafx.geometry.Bounds)}.
     * <p>
     *     Since 3D bounds cannot be converted with {@code rootScene} set to {@code true}, trying to convert 3D bounds will yield {@code null}.
     * </p>
     * @param bounds the bounds
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return local coordinates of the bounds
     * @since JavaFX 8u40
     */
    public Bounds sceneToLocal(Bounds bounds, boolean rootScene) {
        if (!rootScene) {
            return sceneToLocal(bounds);
        }
        if (bounds.getMinZ() != 0 || bounds.getMaxZ() != 0) {
            return null;
        }
        final Point2D p1 = sceneToLocal(bounds.getMinX(), bounds.getMinY(), true);
        final Point2D p2 = sceneToLocal(bounds.getMinX(), bounds.getMaxY(), true);
        final Point2D p3 = sceneToLocal(bounds.getMaxX(), bounds.getMinY(), true);
        final Point2D p4 = sceneToLocal(bounds.getMaxX(), bounds.getMaxY(), true);

        return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
    }

    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     *
     * Note that if this node is in a {@link SubScene}, the arguments should be in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     *
     * @param sceneX x coordinate of a point on a Scene
     * @param sceneY y coordinate of a point on a Scene
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     */
    public Point2D sceneToLocal(double sceneX, double sceneY) {
        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;
        tempPt.setLocation((float)sceneX, (float)sceneY);
        try {
            sceneToLocal(tempPt);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new Point2D(tempPt.x, tempPt.y);
    }

    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     *
     * Note that if this node is in a {@link SubScene}, the arguments should be in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     *
     * @param scenePoint a point on a Scene
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     */
    public Point2D sceneToLocal(Point2D scenePoint) {
        return sceneToLocal(scenePoint.getX(), scenePoint.getY());
    }

    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     *
     * Note that if this node is in a {@link SubScene}, the arguments should be in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     *
     * @param scenePoint a point on a Scene
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     * @since JavaFX 8.0
     */
    public Point3D sceneToLocal(Point3D scenePoint) {
        return sceneToLocal(scenePoint.getX(), scenePoint.getY(), scenePoint.getZ());
    }

    /**
     * Transforms a point from the coordinate space of the scene
     * into the local coordinate space of this {@code Node}.
     *
     * Note that if this node is in a {@link SubScene}, the arguments should be in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     *
     * @param sceneX x coordinate of a point on a Scene
     * @param sceneY y coordinate of a point on a Scene
     * @param sceneZ z coordinate of a point on a Scene
     * @return local Node's coordinates of the point or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     * @since JavaFX 8.0
     */
    public Point3D sceneToLocal(double sceneX, double sceneY, double sceneZ) {
        try {
            return sceneToLocal0(sceneX, sceneY, sceneZ);
        } catch (NoninvertibleTransformException ex) {
            return null;
        }
    }

    /**
     * Internal method to transform a point from scene to local coordinates.
     */
    private Point3D sceneToLocal0(double x, double y, double z) throws NoninvertibleTransformException {
        final com.sun.javafx.geom.Vec3d tempV3D =
                TempState.getInstance().vec3d;
        tempV3D.set(x, y, z);
        sceneToLocal(tempV3D);
        return new Point3D(tempV3D.x, tempV3D.y, tempV3D.z);
    }

    /**
     * Transforms a rectangle from the coordinate space of the
     * scene into the local coordinate space of this
     * {@code Node}.
     *
     * Note that if this node is in a {@link SubScene}, the arguments should be in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     *
     * @param sceneBounds bounds on a Scene
     * @return bounds in the local Node'space or null if Node is not in a {@link Window}.
     * Null is also returned if the transformation from local to Scene is not invertible.
     */
    public Bounds sceneToLocal(Bounds sceneBounds) {
        // Do a quick update of localToParentTransform so that we can determine
        // if this tx is 2D transform
        updateLocalToParentTransform();
        if (localToParentTx.is2D() && (sceneBounds.getMinZ() == 0) && (sceneBounds.getMaxZ() == 0)) {
            Point2D p1 = sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());
            Point2D p2 = sceneToLocal(sceneBounds.getMaxX(), sceneBounds.getMinY());
            Point2D p3 = sceneToLocal(sceneBounds.getMaxX(), sceneBounds.getMaxY());
            Point2D p4 = sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        try {
            Point3D p1 = sceneToLocal0(sceneBounds.getMinX(), sceneBounds.getMinY(), sceneBounds.getMinZ());
            Point3D p2 = sceneToLocal0(sceneBounds.getMinX(), sceneBounds.getMinY(), sceneBounds.getMaxZ());
            Point3D p3 = sceneToLocal0(sceneBounds.getMinX(), sceneBounds.getMaxY(), sceneBounds.getMinZ());
            Point3D p4 = sceneToLocal0(sceneBounds.getMinX(), sceneBounds.getMaxY(), sceneBounds.getMaxZ());
            Point3D p5 = sceneToLocal0(sceneBounds.getMaxX(), sceneBounds.getMaxY(), sceneBounds.getMinZ());
            Point3D p6 = sceneToLocal0(sceneBounds.getMaxX(), sceneBounds.getMaxY(), sceneBounds.getMaxZ());
            Point3D p7 = sceneToLocal0(sceneBounds.getMaxX(), sceneBounds.getMinY(), sceneBounds.getMinZ());
            Point3D p8 = sceneToLocal0(sceneBounds.getMaxX(), sceneBounds.getMinY(), sceneBounds.getMaxZ());
            return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its {@link javafx.stage.Screen}.
     * @param localX x coordinate of a point in Node's space
     * @param localY y coordinate of a point in Node's space
     * @return screen coordinates of the point or null if Node is not in a {@link Window}
     * @since JavaFX 8.0
     */
    public Point2D localToScreen(double localX, double localY) {
        return localToScreen(localX, localY, 0.0);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its {@link javafx.stage.Screen}.
     * @param localPoint a point in Node's space
     * @return screen coordinates of the point or null if Node is not in a {@link Window}
     * @since JavaFX 8.0
     */
    public Point2D localToScreen(Point2D localPoint) {
        return localToScreen(localPoint.getX(), localPoint.getY());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its {@link javafx.stage.Screen}.
     * @param localX x coordinate of a point in Node's space
     * @param localY y coordinate of a point in Node's space
     * @param localZ z coordinate of a point in Node's space
     * @return screen coordinates of the point or null if Node is not in a {@link Window}
     * @since JavaFX 8.0
     */
    public Point2D localToScreen(double localX, double localY, double localZ) {
        Scene scene = getScene();
        if (scene == null) return null;
        Window window = scene.getWindow();
        if (window == null) return null;

        Point3D pt = localToScene(localX, localY, localZ);
        final SubScene subScene = getSubScene();
        if (subScene != null) {
            pt = SceneUtils.subSceneToScene(subScene, pt);
        }
        final Point2D projection = CameraHelper.project(
                SceneHelper.getEffectiveCamera(getScene()), pt);

        return new Point2D(projection.getX() + scene.getX() + window.getX(),
                           projection.getY() + scene.getY() + window.getY());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its {@link javafx.stage.Screen}.
     * @param localPoint a point in Node's space
     * @return screen coordinates of the point or null if Node is not in a {@link Window}
     * @since JavaFX 8.0
     */
    public Point2D localToScreen(Point3D localPoint) {
        return localToScreen(localPoint.getX(), localPoint.getY(), localPoint.getZ());
    }

    /**
     * Transforms a bounds from the local coordinate space of this
     * {@code Node} into the coordinate space of its {@link javafx.stage.Screen}.
     * @param localBounds bounds in Node's space
     * @return the bounds in screen coordinates or null if Node is not in a {@link Window}
     * @since JavaFX 8.0
     */
    public Bounds localToScreen(Bounds localBounds) {
        final Point2D p1 = localToScreen(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMinZ());
        final Point2D p2 = localToScreen(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMaxZ());
        final Point2D p3 = localToScreen(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMinZ());
        final Point2D p4 = localToScreen(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMaxZ());
        final Point2D p5 = localToScreen(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMinZ());
        final Point2D p6 = localToScreen(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMaxZ());
        final Point2D p7 = localToScreen(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMinZ());
        final Point2D p8 = localToScreen(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMaxZ());

        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * Note that if this node is in a {@link SubScene}, the result is in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     * @param localX x coordinate of a point in Node's space
     * @param localY y coordinate of a point in Node's space
     * @return scene coordinates of the point or null if Node is not in a {@link Window}
     */
    public Point2D localToScene(double localX, double localY) {
        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;
        tempPt.setLocation((float)localX, (float)localY);
        localToScene(tempPt);
        return new Point2D(tempPt.x, tempPt.y);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * Note that if this node is in a {@link SubScene}, the result is in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     * @param localPoint a point in Node's space
     * @return scene coordinates of the point or null if Node is not in a {@link Window}
     */
    public Point2D localToScene(Point2D localPoint) {
        return localToScene(localPoint.getX(), localPoint.getY());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * Note that if this node is in a {@link SubScene}, the result is in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     * @param localPoint a 3D point in Node's space
     * @return the transformed 3D point in Scene's space
     * @see #localToScene(javafx.geometry.Point3D, boolean)
     * @since JavaFX 8.0
     */
    public Point3D localToScene(Point3D localPoint) {
        return localToScene(localPoint.getX(), localPoint.getY(), localPoint.getZ());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * Note that if this node is in a {@link SubScene}, the result is in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     * @param x the x coordinate of a point in Node's space
     * @param y the y coordinate of a point in Node's space
     * @param z the z coordinate of a point in Node's space
     * @return the transformed 3D point in Scene's space
     * @see #localToScene(double, double, double, boolean)
     * @since JavaFX 8.0
     */
    public Point3D localToScene(double x, double y, double z) {
        final com.sun.javafx.geom.Vec3d tempV3D =
                TempState.getInstance().vec3d;
        tempV3D.set(x, y, z);
        localToScene(tempV3D);
        return new Point3D(tempV3D.x, tempV3D.y, tempV3D.z);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * result point is in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #localToScene(javafx.geometry.Point3D)}.
     *
     * @param localPoint the point in local coordinates
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return transformed point
     *
     * @see #localToScene(javafx.geometry.Point3D)
     * @since JavaFX 8u40
     */
    public Point3D localToScene(Point3D localPoint, boolean rootScene) {
        Point3D pt = localToScene(localPoint);
        if (rootScene) {
            final SubScene subScene = getSubScene();
            if (subScene != null) {
                pt = SceneUtils.subSceneToScene(subScene, pt);
            }
        }
        return pt;
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * result point is in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #localToScene(double, double, double)}.
     *
     * @param x the x coordinate of the point in local coordinates
     * @param y the y coordinate of the point in local coordinates
     * @param z the z coordinate of the point in local coordinates
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return transformed point
     *
     * @see #localToScene(double, double, double)
     * @since JavaFX 8u40
     */
    public Point3D localToScene(double x, double y, double z, boolean rootScene) {
        return localToScene(new Point3D(x, y, z), rootScene);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * result point is in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #localToScene(javafx.geometry.Point2D)}.
     *
     * @param localPoint the point in local coordinates
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return transformed point
     *
     * @see #localToScene(javafx.geometry.Point2D)
     * @since JavaFX 8u40
     */
    public Point2D localToScene(Point2D localPoint, boolean rootScene) {
        if (!rootScene) {
            return localToScene(localPoint);
        }
        Point3D pt = localToScene(localPoint.getX(), localPoint.getY(), 0, rootScene);
        return new Point2D(pt.getX(), pt.getY());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * result point is in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #localToScene(double, double)}.
     *
     * @param x the x coordinate of the point in local coordinates
     * @param y the y coordinate of the point in local coordinates
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return transformed point
     *
     * @see #localToScene(double, double)
     * @since JavaFX 8u40
     */
    public Point2D localToScene(double x, double y, boolean rootScene) {
        return localToScene(new Point2D(x, y), rootScene);
    }

    /**
     * Transforms a bounds from the local coordinate space of this {@code Node}
     * into the coordinate space of its scene.
     * If the Node does not have any {@link SubScene} or {@code rootScene} is set to true, the
     * result bounds are in {@link Scene} coordinates of the Node returned by {@link #getScene()}.
     * Otherwise, the subscene coordinates are used, which is equivalent to calling
     * {@link #localToScene(javafx.geometry.Bounds)}.
     *
     * @param localBounds the bounds in local coordinates
     * @param rootScene whether Scene coordinates should be used even if the Node is in a SubScene
     * @return transformed bounds
     *
     * @see #localToScene(javafx.geometry.Bounds)
     * @since JavaFX 8u40
     */
    public Bounds localToScene(Bounds localBounds, boolean rootScene) {
        if (!rootScene) {
            return localToScene(localBounds);
        }
        Point3D p1 = localToScene(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMinZ(), true);
        Point3D p2 = localToScene(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMaxZ(), true);
        Point3D p3 = localToScene(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMinZ(), true);
        Point3D p4 = localToScene(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMaxZ(), true);
        Point3D p5 = localToScene(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMinZ(), true);
        Point3D p6 = localToScene(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMaxZ(), true);
        Point3D p7 = localToScene(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMinZ(), true);
        Point3D p8 = localToScene(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMaxZ(), true);
        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Transforms a bounds from the local coordinate space of this
     * {@code Node} into the coordinate space of its scene.
     * Note that if this node is in a {@link SubScene}, the result is in the subscene coordinates,
     * not that of {@link javafx.scene.Scene}.
     * @param localBounds bounds in Node's space
     * @return the bounds in the scene coordinates or null if Node is not in a {@link Window}
     * @see #localToScene(javafx.geometry.Bounds, boolean)
     */
    public Bounds localToScene(Bounds localBounds) {
        // Do a quick update of localToParentTransform so that we can determine
        // if this tx is 2D transform
        updateLocalToParentTransform();
        if (localToParentTx.is2D() && (localBounds.getMinZ() == 0) && (localBounds.getMaxZ() == 0)) {
            Point2D p1 = localToScene(localBounds.getMinX(), localBounds.getMinY());
            Point2D p2 = localToScene(localBounds.getMaxX(), localBounds.getMinY());
            Point2D p3 = localToScene(localBounds.getMaxX(), localBounds.getMaxY());
            Point2D p4 = localToScene(localBounds.getMinX(), localBounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        Point3D p1 = localToScene(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMinZ());
        Point3D p2 = localToScene(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMaxZ());
        Point3D p3 = localToScene(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMinZ());
        Point3D p4 = localToScene(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMaxZ());
        Point3D p5 = localToScene(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMinZ());
        Point3D p6 = localToScene(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMaxZ());
        Point3D p7 = localToScene(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMinZ());
        Point3D p8 = localToScene(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMaxZ());
        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);

    }

    /**
     * Transforms a point from the coordinate space of the parent into the
     * local coordinate space of this {@code Node}.
     * @param parentX the x coordinate in Parent's space
     * @param parentY the y coordinate in Parent's space
     * @return the transformed 2D point in Node's space
     */
    public Point2D parentToLocal(double parentX, double parentY) {
        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;
        tempPt.setLocation((float)parentX, (float)parentY);
        try {
            parentToLocal(tempPt);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new Point2D(tempPt.x, tempPt.y);
    }

    /**
     * Transforms a point from the coordinate space of the parent into the
     * local coordinate space of this {@code Node}.
     * @param parentPoint the 2D point in Parent's space
     * @return the transformed 2D point in Node's space
     */
    public Point2D parentToLocal(Point2D parentPoint) {
        return parentToLocal(parentPoint.getX(), parentPoint.getY());
    }

    /**
     * Transforms a point from the coordinate space of the parent into the
     * local coordinate space of this {@code Node}.
     * @param parentPoint parentPoint the 3D point in Parent's space
     * @return the transformed 3D point in Node's space
     * @since JavaFX 8.0
     */
    public Point3D parentToLocal(Point3D parentPoint) {
        return parentToLocal(parentPoint.getX(), parentPoint.getY(), parentPoint.getZ());
    }

    /**
     * Transforms a point from the coordinate space of the parent into the
     * local coordinate space of this {@code Node}.
     * @param parentX the x coordinate in Parent's space
     * @param parentY the y coordinate in Parent's space
     * @param parentZ the z coordinate in Parent's space
     * @return the transformed 3D point in Node's space
     * @since JavaFX 8.0
     */
    public Point3D parentToLocal(double parentX, double parentY, double parentZ) {
        final com.sun.javafx.geom.Vec3d tempV3D =
                TempState.getInstance().vec3d;
        tempV3D.set(parentX, parentY, parentZ);
        try {
            parentToLocal(tempV3D);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new Point3D(tempV3D.x, tempV3D.y, tempV3D.z);
    }

    /**
     * Transforms a rectangle from the coordinate space of the parent into the
     * local coordinate space of this {@code Node}.
     * @param parentBounds the bounds in Parent's space
     * @return the transformed bounds in Node's space
     */
    public Bounds parentToLocal(Bounds parentBounds) {
        // Do a quick update of localToParentTransform so that we can determine
        // if this tx is 2D transform
        updateLocalToParentTransform();
        if (localToParentTx.is2D() && (parentBounds.getMinZ() == 0) && (parentBounds.getMaxZ() == 0)) {
            Point2D p1 = parentToLocal(parentBounds.getMinX(), parentBounds.getMinY());
            Point2D p2 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMinY());
            Point2D p3 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMaxY());
            Point2D p4 = parentToLocal(parentBounds.getMinX(), parentBounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        Point3D p1 = parentToLocal(parentBounds.getMinX(), parentBounds.getMinY(), parentBounds.getMinZ());
        Point3D p2 = parentToLocal(parentBounds.getMinX(), parentBounds.getMinY(), parentBounds.getMaxZ());
        Point3D p3 = parentToLocal(parentBounds.getMinX(), parentBounds.getMaxY(), parentBounds.getMinZ());
        Point3D p4 = parentToLocal(parentBounds.getMinX(), parentBounds.getMaxY(), parentBounds.getMaxZ());
        Point3D p5 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMaxY(), parentBounds.getMinZ());
        Point3D p6 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMaxY(), parentBounds.getMaxZ());
        Point3D p7 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMinY(), parentBounds.getMinZ());
        Point3D p8 = parentToLocal(parentBounds.getMaxX(), parentBounds.getMinY(), parentBounds.getMaxZ());
        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its parent.
     * @param localX the x coordinate of the point in Node's space
     * @param localY the y coordinate of the point in Node's space
     * @return the transformed 2D point in Parent's space
     */
    public Point2D localToParent(double localX, double localY) {
        final com.sun.javafx.geom.Point2D tempPt =
                TempState.getInstance().point;
        tempPt.setLocation((float)localX, (float)localY);
        localToParent(tempPt);
        return new Point2D(tempPt.x, tempPt.y);
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its parent.
     * @param localPoint the 2D point in Node's space
     * @return the transformed 2D point in Parent's space
     */
    public Point2D localToParent(Point2D localPoint) {
        return localToParent(localPoint.getX(), localPoint.getY());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its parent.
     * @param localPoint the 3D point in Node's space
     * @return the transformed 3D point in Parent's space
     * @since JavaFX 8.0
     */
    public Point3D localToParent(Point3D localPoint) {
        return localToParent(localPoint.getX(), localPoint.getY(), localPoint.getZ());
    }

    /**
     * Transforms a point from the local coordinate space of this {@code Node}
     * into the coordinate space of its parent.
     * @param x the x coordinate of the point in Node's space
     * @param y the y coordinate of the point in Node's space
     * @param z the z coordinate of the point in Node's space
     * @return the transformed 3D point in Parent's space
     * @since JavaFX 8.0
     */
    public Point3D localToParent(double x, double y, double z) {
        final com.sun.javafx.geom.Vec3d tempV3D =
                TempState.getInstance().vec3d;
        tempV3D.set(x, y, z);
        localToParent(tempV3D);
        return new Point3D(tempV3D.x, tempV3D.y, tempV3D.z);
    }

    /**
     * Transforms a bounds from the local coordinate space of this
     * {@code Node} into the coordinate space of its parent.
     * @param localBounds the bounds in Node's space
     * @return the transformed bounds in Parent's space
     */
    public Bounds localToParent(Bounds localBounds) {
        // Do a quick update of localToParentTransform so that we can determine
        // if this tx is 2D transform
        updateLocalToParentTransform();
        if (localToParentTx.is2D() && (localBounds.getMinZ() == 0) && (localBounds.getMaxZ() == 0)) {
            Point2D p1 = localToParent(localBounds.getMinX(), localBounds.getMinY());
            Point2D p2 = localToParent(localBounds.getMaxX(), localBounds.getMinY());
            Point2D p3 = localToParent(localBounds.getMaxX(), localBounds.getMaxY());
            Point2D p4 = localToParent(localBounds.getMinX(), localBounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        Point3D p1 = localToParent(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMinZ());
        Point3D p2 = localToParent(localBounds.getMinX(), localBounds.getMinY(), localBounds.getMaxZ());
        Point3D p3 = localToParent(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMinZ());
        Point3D p4 = localToParent(localBounds.getMinX(), localBounds.getMaxY(), localBounds.getMaxZ());
        Point3D p5 = localToParent(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMinZ());
        Point3D p6 = localToParent(localBounds.getMaxX(), localBounds.getMaxY(), localBounds.getMaxZ());
        Point3D p7 = localToParent(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMinZ());
        Point3D p8 = localToParent(localBounds.getMaxX(), localBounds.getMinY(), localBounds.getMaxZ());
        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Copy the localToParent transform into specified transform.
     */
    BaseTransform getLocalToParentTransform(BaseTransform tx) {
        updateLocalToParentTransform();
        tx.setTransform(localToParentTx);
        return tx;
    }

    /*
     * Currently used only by PathTransition
     */
    final BaseTransform getLeafTransform() {
        return getLocalToParentTransform(TempState.getInstance().leafTx);
    }

    /*
     * Invoked whenever the transforms[] ObservableList changes, or by the transforms
     * in that ObservableList whenever they are changed.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doTransformsChanged() {
        if (!transformDirty) {
            NodeHelper.markDirty(this, DirtyBits.NODE_TRANSFORM);
            transformDirty = true;
            transformedBoundsChanged();
        }
        invalidateLocalToParentTransform();
        invalidateLocalToSceneTransform();
    }

    final double getPivotX() {
        final Bounds bounds = getLayoutBounds();
        return bounds.getMinX() + bounds.getWidth()/2;
    }

    final double getPivotY() {
        final Bounds bounds = getLayoutBounds();
        return bounds.getMinY() + bounds.getHeight()/2;
    }

    final double getPivotZ() {
        final Bounds bounds = getLayoutBounds();
        return bounds.getMinZ() + bounds.getDepth()/2;
    }

    /**
     * This helper function will update the transform matrix on the peer based
     * on the "complete" transform for this node.
     */
    void updateLocalToParentTransform() {
        if (transformDirty) {
            localToParentTx.setToIdentity();

            boolean mirror = false;
            double mirroringCenter = 0;
            if (hasMirroring()) {
                final Scene sceneValue = getScene();
                if ((sceneValue != null) && (sceneValue.getRoot() == this)) {
                    // handle scene mirroring in this branch
                    // (must be the last transformation)
                    mirroringCenter = sceneValue.getWidth() / 2;
                    if (mirroringCenter == 0.0) {
                        mirroringCenter = getPivotX();
                    }

                    localToParentTx = localToParentTx.deriveWithTranslation(
                            mirroringCenter, 0.0);
                    localToParentTx = localToParentTx.deriveWithScale(
                            -1.0, 1.0, 1.0);
                    localToParentTx = localToParentTx.deriveWithTranslation(
                            -mirroringCenter, 0.0);
                } else {
                    // mirror later
                    mirror = true;
                    mirroringCenter = getPivotX();
                }
            }

            if (getScaleX() != 1 || getScaleY() != 1 || getScaleZ() != 1 || getRotate() != 0) {
                // recompute pivotX, pivotY and pivotZ
                double pivotX = getPivotX();
                double pivotY = getPivotY();
                double pivotZ = getPivotZ();

                localToParentTx = localToParentTx.deriveWithTranslation(
                        getTranslateX() + getLayoutX() + pivotX,
                        getTranslateY() + getLayoutY() + pivotY,
                        getTranslateZ() + pivotZ);
                localToParentTx = localToParentTx.deriveWithRotation(
                        Math.toRadians(getRotate()), getRotationAxis().getX(),
                        getRotationAxis().getY(), getRotationAxis().getZ());
                localToParentTx = localToParentTx.deriveWithScale(
                        getScaleX(), getScaleY(), getScaleZ());
                localToParentTx = localToParentTx.deriveWithTranslation(
                        -pivotX, -pivotY, -pivotZ);
            } else {
                localToParentTx = localToParentTx.deriveWithTranslation(
                        getTranslateX() + getLayoutX(),
                        getTranslateY() + getLayoutY(),
                        getTranslateZ());
            }

            if (hasTransforms()) {
                for (Transform t : getTransforms()) {
                    localToParentTx = TransformHelper.derive(t, localToParentTx);
                }
            }

            // Check to see whether the node requires mirroring
            if (mirror) {
                localToParentTx = localToParentTx.deriveWithTranslation(
                        mirroringCenter, 0);
                localToParentTx = localToParentTx.deriveWithScale(
                        -1.0, 1.0, 1.0);
                localToParentTx = localToParentTx.deriveWithTranslation(
                        -mirroringCenter, 0);
            }

            transformDirty = false;
        }
    }

    /**
     * Transforms in place the specified point from parent coords to local
     * coords. Made package private for the sake of testing.
     */
    void parentToLocal(com.sun.javafx.geom.Point2D pt) throws NoninvertibleTransformException {
        updateLocalToParentTransform();
        localToParentTx.inverseTransform(pt, pt);
    }

    void parentToLocal(com.sun.javafx.geom.Vec3d pt) throws NoninvertibleTransformException {
        updateLocalToParentTransform();
        localToParentTx.inverseTransform(pt, pt);
    }

    void sceneToLocal(com.sun.javafx.geom.Point2D pt) throws NoninvertibleTransformException {
        if (getParent() != null) {
            getParent().sceneToLocal(pt);
        }
        parentToLocal(pt);
    }

    void sceneToLocal(com.sun.javafx.geom.Vec3d pt) throws NoninvertibleTransformException {
        if (getParent() != null) {
            getParent().sceneToLocal(pt);
        }
        parentToLocal(pt);
    }

    void localToScene(com.sun.javafx.geom.Point2D pt) {
        localToParent(pt);
        if (getParent() != null) {
            getParent().localToScene(pt);
        }
    }

    void localToScene(com.sun.javafx.geom.Vec3d pt) {
        localToParent(pt);
        if (getParent() != null) {
            getParent().localToScene(pt);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Mouse event related APIs                                                *
     *                                                                         *
     **************************************************************************/

    /**
     * Transforms in place the specified point from local coords to parent
     * coords. Made package private for the sake of testing.
     */
    void localToParent(com.sun.javafx.geom.Point2D pt) {
        updateLocalToParentTransform();
        localToParentTx.transform(pt, pt);
    }

    void localToParent(com.sun.javafx.geom.Vec3d pt) {
        updateLocalToParentTransform();
        localToParentTx.transform(pt, pt);
    }

    /*
     * Finds a top-most child node that contains the given local coordinates.
     *
     * The result argument is used for storing the picking result.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doPickNodeLocal(PickRay localPickRay, PickResultChooser result) {
        intersects(localPickRay, result);
    }

    /*
     * Finds a top-most child node that intersects the given ray.
     *
     * The result argument is used for storing the picking result.
     */
    final void pickNode(PickRay pickRay, PickResultChooser result) {

        // In some conditions we can omit picking this node or subgraph
        if (!isVisible() || isDisable() || isMouseTransparent()) {
            return;
        }

        final Vec3d o = pickRay.getOriginNoClone();
        final double ox = o.x;
        final double oy = o.y;
        final double oz = o.z;
        final Vec3d d = pickRay.getDirectionNoClone();
        final double dx = d.x;
        final double dy = d.y;
        final double dz = d.z;

        updateLocalToParentTransform();
        try {
            localToParentTx.inverseTransform(o, o);
            localToParentTx.inverseDeltaTransform(d, d);

            // Delegate to a function which can be overridden by subclasses which
            // actually does the pick. The implementation is markedly different
            // for leaf nodes vs. parent nodes vs. region nodes.
            NodeHelper.pickNodeLocal(this, pickRay, result);
        } catch (NoninvertibleTransformException e) {
            // in this case we just don't pick anything
        }

        pickRay.setOrigin(ox, oy, oz);
        pickRay.setDirection(dx, dy, dz);
    }

    /*
     * Returns {@code true} if the given ray (start, dir), specified in the
     * local coordinate space of this {@code Node}, intersects the
     * shape of this {@code Node}. Note that this method does not take visibility
     * into account; the test is based on the geometry of this {@code Node} only.
     * <p>
     * The pickResult is updated if the found intersection is closer than
     * the currently held one.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     */
    final boolean intersects(PickRay pickRay, PickResultChooser pickResult) {
        double boundsDistance = intersectsBounds(pickRay);
        if (!Double.isNaN(boundsDistance)) {
            if (isPickOnBounds()) {
                if (pickResult != null) {
                    pickResult.offer(this, boundsDistance, PickResultChooser.computePoint(pickRay, boundsDistance));
                }
                return true;
            } else {
                return NodeHelper.computeIntersects(this, pickRay, pickResult);
            }
        }
        return false;
    }

    /*
     * Computes the intersection of the pickRay with this node.
     * The pickResult argument is updated if the found intersection
     * is closer than the passed one. On the other hand, the return value
     * specifies whether the intersection exists, regardless of its comparison
     * with the given pickResult.
     */
    private boolean doComputeIntersects(PickRay pickRay, PickResultChooser pickResult) {
        double origZ = pickRay.getOriginNoClone().z;
        double dirZ = pickRay.getDirectionNoClone().z;
        // Handle the case where pickRay is almost parallel to the Z-plane
        if (almostZero(dirZ)) {
            return false;
        }
        double t = -origZ / dirZ;
        if (t < pickRay.getNearClip() || t > pickRay.getFarClip()) {
            return false;
        }
        double x = pickRay.getOriginNoClone().x + (pickRay.getDirectionNoClone().x * t);
        double y = pickRay.getOriginNoClone().y + (pickRay.getDirectionNoClone().y * t);

        if (contains((float) x, (float) y)) {
            if (pickResult != null) {
                pickResult.offer(this, t, PickResultChooser.computePoint(pickRay, t));
            }
            return true;
        }
        return false;
    }

    /*
     * Computes the intersection of the pickRay with the bounds of this node.
     * The return value is the distance between the camera and the intersection
     * point, measured in pickRay direction magnitudes. If there is
     * no intersection, it returns NaN.
     *
     * @param pickRay The pick ray
     * @return Distance of the intersection point, a NaN if there
     *         is no intersection
     */
    final double intersectsBounds(PickRay pickRay) {

        final Vec3d dir = pickRay.getDirectionNoClone();
        double tmin, tmax;

        final Vec3d origin = pickRay.getOriginNoClone();
        final double originX = origin.x;
        final double originY = origin.y;
        final double originZ = origin.z;

        final TempState tempState = TempState.getInstance();
        BaseBounds tempBounds = tempState.bounds;

        tempBounds = getLocalBounds(tempBounds,
                                    BaseTransform.IDENTITY_TRANSFORM);

        if (dir.x == 0.0 && dir.y == 0.0) {
            // fast path for the usual 2D picking

            if (dir.z == 0.0) {
                return Double.NaN;
            }

            if (originX < tempBounds.getMinX() ||
                    originX > tempBounds.getMaxX() ||
                    originY < tempBounds.getMinY() ||
                    originY > tempBounds.getMaxY()) {
                return Double.NaN;
            }

            final double invDirZ = 1.0 / dir.z;
            final boolean signZ = invDirZ < 0.0;

            final double minZ = tempBounds.getMinZ();
            final double maxZ = tempBounds.getMaxZ();
            tmin = ((signZ ? maxZ : minZ) - originZ) * invDirZ;
            tmax = ((signZ ? minZ : maxZ) - originZ) * invDirZ;

        } else if (tempBounds.getDepth() == 0.0) {
            // fast path for 3D picking of 2D bounds

            if (almostZero(dir.z)) {
                return Double.NaN;
            }

            final double t = (tempBounds.getMinZ() - originZ) / dir.z;
            final double x = originX + (dir.x * t);
            final double y = originY + (dir.y * t);

            if (x < tempBounds.getMinX() ||
                    x > tempBounds.getMaxX() ||
                    y < tempBounds.getMinY() ||
                    y > tempBounds.getMaxY()) {
                return Double.NaN;
            }

            tmin = tmax = t;

        } else {

            final double invDirX = dir.x == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.x);
            final double invDirY = dir.y == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.y);
            final double invDirZ = dir.z == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.z);
            final boolean signX = invDirX < 0.0;
            final boolean signY = invDirY < 0.0;
            final boolean signZ = invDirZ < 0.0;
            final double minX = tempBounds.getMinX();
            final double minY = tempBounds.getMinY();
            final double maxX = tempBounds.getMaxX();
            final double maxY = tempBounds.getMaxY();

            tmin = Double.NEGATIVE_INFINITY;
            tmax = Double.POSITIVE_INFINITY;
            if (Double.isInfinite(invDirX)) {
                if (minX <= originX && maxX >= originX) {
                    // move on, we are inside for the whole length
                } else {
                    return Double.NaN;
                }
            } else {
                tmin = ((signX ? maxX : minX) - originX) * invDirX;
                tmax = ((signX ? minX : maxX) - originX) * invDirX;
            }

            if (Double.isInfinite(invDirY)) {
                if (minY <= originY && maxY >= originY) {
                    // move on, we are inside for the whole length
                } else {
                    return Double.NaN;
                }
            } else {
                final double tymin = ((signY ? maxY : minY) - originY) * invDirY;
                final double tymax = ((signY ? minY : maxY) - originY) * invDirY;

                if ((tmin > tymax) || (tymin > tmax)) {
                    return Double.NaN;
                }
                if (tymin > tmin) {
                    tmin = tymin;
                }
                if (tymax < tmax) {
                    tmax = tymax;
                }
            }

            final double minZ = tempBounds.getMinZ();
            final double maxZ = tempBounds.getMaxZ();
            if (Double.isInfinite(invDirZ)) {
                if (minZ <= originZ && maxZ >= originZ) {
                    // move on, we are inside for the whole length
                } else {
                    return Double.NaN;
                }
            } else {
                final double tzmin = ((signZ ? maxZ : minZ) - originZ) * invDirZ;
                final double tzmax = ((signZ ? minZ : maxZ) - originZ) * invDirZ;

                if ((tmin > tzmax) || (tzmin > tmax)) {
                    return Double.NaN;
                }
                if (tzmin > tmin) {
                    tmin = tzmin;
                }
                if (tzmax < tmax) {
                    tmax = tzmax;
                }
            }
        }

        // For clip we use following semantics: pick the node normally
        // if there is an intersection with the clip node. We don't consider
        // clip node distance.
        Node clip = getClip();
        if (clip != null
                // FIXME: All 3D picking is currently ignored by rendering.
                // Until this is fixed or defined differently (JDK-8090485),
                // we follow this behavior.
                && !(this instanceof Shape3D) && !(clip instanceof Shape3D)) {
            final double dirX = dir.x;
            final double dirY = dir.y;
            final double dirZ = dir.z;

            clip.updateLocalToParentTransform();

            boolean hitClip = true;
            try {
                clip.localToParentTx.inverseTransform(origin, origin);
                clip.localToParentTx.inverseDeltaTransform(dir, dir);
            } catch (NoninvertibleTransformException e) {
                hitClip = false;
            }
            hitClip = hitClip && clip.intersects(pickRay, null);
            pickRay.setOrigin(originX, originY, originZ);
            pickRay.setDirection(dirX, dirY, dirZ);

            if (!hitClip) {
                return Double.NaN;
            }
        }

        if (Double.isInfinite(tmin) || Double.isNaN(tmin)) {
            // We've got a nonsense pick ray or bounds.
            return Double.NaN;
        }

        final double minDistance = pickRay.getNearClip();
        final double maxDistance = pickRay.getFarClip();
        if (tmin < minDistance) {
            if (tmax >= minDistance) {
                // we are inside bounds
                return 0.0;
            } else {
                return Double.NaN;
            }
        } else if (tmin > maxDistance) {
            return Double.NaN;
        }

        return tmin;
    }


    // Good to find a home for commonly use util. code such as EPS.
    // and almostZero. This code currently defined in multiple places,
    // such as Affine3D and GeneralTransform3D.
    private static final double EPSILON_ABSOLUTE = 1.0e-5;

    static boolean almostZero(double a) {
        return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
    }

    /* *************************************************************************
     *                                                                         *
     *                      viewOrder property handling                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines the rendering and picking order of this {@code Node} within its
     * parent.
     * <p>
     * This property is used to alter the rendering and picking order of a node
     * within its parent without reordering the parent's {@code children} list.
     * For example, this can be used as a more efficient way to implement
     * transparency sorting. To do this, an application can assign the viewOrder
     * value of each node to the computed distance between that node and the
     * viewer.
     * </p>
     * <p>
     * The parent will traverse its {@code children} in decreasing
     * {@code viewOrder} order. This means that a child with a lower
     * {@code viewOrder} will be in front of a child with a higher
     * {@code viewOrder}. If two children have the same {@code viewOrder}, the
     * parent will traverse them in the order they appear in the parent's
     * {@code children} list.
     * </p>
     * <p>
     * However, {@code viewOrder} does not alter the layout and focus traversal
     * order of this Node within its parent. A parent always traverses its
     * {@code children} list in order when doing layout or focus traversal.
     * </p>
     *
     * @return the view order for this {@code Node}
     * @defaultValue 0.0
     *
     * @since 9
     */
    public final DoubleProperty viewOrderProperty() {
        return getMiscProperties().viewOrderProperty();
    }

    public final void setViewOrder(double value) {
        viewOrderProperty().set(value);
    }

    public final double getViewOrder() {
        return (miscProperties == null) ? DEFAULT_VIEW_ORDER
                : miscProperties.getViewOrder();
    }

    /* *************************************************************************
     *                                                                         *
     *                             Transformations                             *
     *                                                                         *
     **************************************************************************/
    /**
     * The {@code ObservableList} of custom {@link javafx.scene.transform.Transform}s
     * to be applied to this {@code Node}. These transforms are applied before the predefined transforms.
     * <p>
     * See also the <a href="#Transformations">Transformations</a> section.
     *
     * @return the transforms for this {@code Node}
     * @defaultValue empty
     */
    public final ObservableList<Transform> getTransforms() {
        return transformsProperty();
    }

    private ObservableList<Transform> transformsProperty() {
        return getNodeTransformation().getTransforms();
    }

    public final void setTranslateX(double value) {
        translateXProperty().set(value);
    }

    public final double getTranslateX() {
        return (nodeTransformation == null)
                ? DEFAULT_TRANSLATE_X
                : nodeTransformation.getTranslateX();
    }

    /**
     * Defines the x coordinate of the translation that is added to this {@code Node}'s
     * transform.
     * <p>
     * The node's final translation will be computed as {@link #layoutXProperty layoutX} + {@code translateX},
     * where {@code layoutX} establishes the node's stable position and {@code translateX}
     * optionally makes dynamic adjustments to that position.
     *<p>
     * This variable can be used to alter the location of a node without disturbing
     * its {@link #layoutBoundsProperty layoutBounds}, which makes it useful for animating a node's location.
     *
     * @return the translateX for this {@code Node}
     * @defaultValue 0
     */
    public final DoubleProperty translateXProperty() {
        return getNodeTransformation().translateXProperty();
    }

    public final void setTranslateY(double value) {
        translateYProperty().set(value);
    }

    public final double getTranslateY() {
        return (nodeTransformation == null)
                ? DEFAULT_TRANSLATE_Y
                : nodeTransformation.getTranslateY();
    }

    /**
     * Defines the y coordinate of the translation that is added to this {@code Node}'s
     * transform.
     * <p>
     * The node's final translation will be computed as {@link #layoutYProperty layoutY} + {@code translateY},
     * where {@code layoutY} establishes the node's stable position and {@code translateY}
     * optionally makes dynamic adjustments to that position.
     * <p>
     * This variable can be used to alter the location of a node without disturbing
     * its {@link #layoutBoundsProperty layoutBounds}, which makes it useful for animating a node's location.
     *
     * @return the translateY for this {@code Node}
     * @defaultValue 0
     */
    public final DoubleProperty translateYProperty() {
        return getNodeTransformation().translateYProperty();
    }

    public final void setTranslateZ(double value) {
        translateZProperty().set(value);
    }

    public final double getTranslateZ() {
        return (nodeTransformation == null)
                ? DEFAULT_TRANSLATE_Z
                : nodeTransformation.getTranslateZ();
    }

    /**
     * Defines the Z coordinate of the translation that is added to the
     * transformed coordinates of this {@code Node}.  This value will be added
     * to any translation defined by the {@code transforms} ObservableList and
     * {@code layoutZ}.
     * <p>
     * This variable can be used to alter the location of a Node without
     * disturbing its layout bounds, which makes it useful for animating a
     * node's location.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @return the translateZ for this {@code Node}
     * @defaultValue 0
     */
    public final DoubleProperty translateZProperty() {
        return getNodeTransformation().translateZProperty();
    }

    public final void setScaleX(double value) {
        scaleXProperty().set(value);
    }

    public final double getScaleX() {
        return (nodeTransformation == null) ? DEFAULT_SCALE_X
                                            : nodeTransformation.getScaleX();
    }

    /**
     * Defines the factor by which coordinates are scaled about the center of the
     * object along the X axis of this {@code Node}. This is used to stretch or
     * shrink the node either manually or by using an animation.
     * <p>
     * This scale factor is not included in {@link #layoutBoundsProperty layoutBounds} by
     * default, which makes it ideal for scaling the entire node after
     * all effects and transforms have been taken into account.
     * <p>
     * The pivot point about which the scale occurs is the center of the
     * untransformed {@link #layoutBoundsProperty layoutBounds}.
     *
     * @return the scaleX for this {@code Node}
     * @defaultValue 1.0
     */
    public final DoubleProperty scaleXProperty() {
        return getNodeTransformation().scaleXProperty();
    }

    public final void setScaleY(double value) {
        scaleYProperty().set(value);
    }

    public final double getScaleY() {
        return (nodeTransformation == null) ? DEFAULT_SCALE_Y
                                            : nodeTransformation.getScaleY();
    }

    /**
     * Defines the factor by which coordinates are scaled about the center of the
     * object along the Y axis of this {@code Node}. This is used to stretch or
     * shrink the node either manually or by using an animation.
     * <p>
     * This scale factor is not included in {@link #layoutBoundsProperty layoutBounds} by
     * default, which makes it ideal for scaling the entire node after
     * all effects and transforms have been taken into account.
     * <p>
     * The pivot point about which the scale occurs is the center of the
     * untransformed {@link #layoutBoundsProperty layoutBounds}.
     *
     * @return the scaleY for this {@code Node}
     * @defaultValue 1.0
     */
    public final DoubleProperty scaleYProperty() {
        return getNodeTransformation().scaleYProperty();
    }

    public final void setScaleZ(double value) {
        scaleZProperty().set(value);
    }

    public final double getScaleZ() {
        return (nodeTransformation == null) ? DEFAULT_SCALE_Z
                                            : nodeTransformation.getScaleZ();
    }

    /**
     * Defines the factor by which coordinates are scaled about the center of the
     * object along the Z axis of this {@code Node}. This is used to stretch or
     * shrink the node either manually or by using an animation.
     * <p>
     * This scale factor is not included in {@link #layoutBoundsProperty layoutBounds} by
     * default, which makes it ideal for scaling the entire node after
     * all effects and transforms have been taken into account.
     * <p>
     * The pivot point about which the scale occurs is the center of the
     * rectangular bounds formed by taking {@link #boundsInLocalProperty boundsInLocal} and applying
     * all the transforms in the {@link #getTransforms transforms} ObservableList.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @return the scaleZ for this {@code Node}
     * @defaultValue 1.0
     */
    public final DoubleProperty scaleZProperty() {
        return getNodeTransformation().scaleZProperty();
    }

    public final void setRotate(double value) {
        rotateProperty().set(value);
    }

    public final double getRotate() {
        return (nodeTransformation == null) ? DEFAULT_ROTATE
                                            : nodeTransformation.getRotate();
    }

    /**
     * Defines the angle of rotation about the {@code Node}'s center, measured in
     * degrees. This is used to rotate the {@code Node}.
     * <p>
     * This rotation factor is not included in {@link #layoutBoundsProperty layoutBounds} by
     * default, which makes it ideal for rotating the entire node after
     * all effects and transforms have been taken into account.
     * <p>
     * The pivot point about which the rotation occurs is the center of the
     * untransformed {@link #layoutBoundsProperty layoutBounds}.
     * <p>
     * Note that because the pivot point is computed as the center of this
     * {@code Node}'s layout bounds, any change to the layout bounds will cause
     * the pivot point to change, which can move the object. For a leaf node,
     * any change to the geometry will cause the layout bounds to change.
     * For a group node, any change to any of its children, including a
     * change in a child's geometry, clip, effect, position, orientation, or
     * scale, will cause the group's layout bounds to change. If this movement
     * of the pivot point is not
     * desired, applications should instead use the Node's {@link #getTransforms transforms}
     * ObservableList, and add a {@link javafx.scene.transform.Rotate} transform,
     * which has a user-specifiable pivot point.
     *
     * @return the rotate for this {@code Node}
     * @defaultValue 0.0
     */
    public final DoubleProperty rotateProperty() {
        return getNodeTransformation().rotateProperty();
    }

    public final void setRotationAxis(Point3D value) {
        rotationAxisProperty().set(value);
    }

    public final Point3D getRotationAxis() {
        return (nodeTransformation == null)
                ? DEFAULT_ROTATION_AXIS
                : nodeTransformation.getRotationAxis();
    }

    /**
     * Defines the axis of rotation of this {@code Node}.
     * <p>
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @return the rotationAxis for this {@code Node}
     * @defaultValue Rotate.Z_AXIS
     */
    public final ObjectProperty<Point3D> rotationAxisProperty() {
        return getNodeTransformation().rotationAxisProperty();
    }

    /**
     * An affine transform that holds the computed local-to-parent transform.
     * This is the concatenation of all transforms in this node, including all
     * of the convenience transforms.
     * @return the localToParent transform for this {@code Node}
     * @since JavaFX 2.2
     */
    public final ReadOnlyObjectProperty<Transform> localToParentTransformProperty() {
        return getNodeTransformation().localToParentTransformProperty();
    }

    private void invalidateLocalToParentTransform() {
        if (nodeTransformation != null) {
            nodeTransformation.invalidateLocalToParentTransform();
        }
    }

    public final Transform getLocalToParentTransform() {
        return localToParentTransformProperty().get();
    }

    /**
     * An affine transform that holds the computed local-to-scene transform.
     * This is the concatenation of all transforms in this node's parents and
     * in this node, including all of the convenience transforms up to the root.
     * If this node is in a {@link javafx.scene.SubScene}, this property represents
     * transforms up to the subscene, not the root scene.
     *
     * <p>
     * Note that when you register a listener or a binding to this property,
     * it needs to listen for invalidation on all its parents to the root node.
     * This means that registering a listener on this
     * property on many nodes may negatively affect performance of
     * transformation changes in their common parents.
     * </p>
     *
     * @return the localToScene transform for this {@code Node}
     * @since JavaFX 2.2
     */
    public final ReadOnlyObjectProperty<Transform> localToSceneTransformProperty() {
        return getNodeTransformation().localToSceneTransformProperty();
    }

    private void invalidateLocalToSceneTransform() {
        if (nodeTransformation != null) {
            nodeTransformation.invalidateLocalToSceneTransform();
        }
    }

    public final Transform getLocalToSceneTransform() {
        return localToSceneTransformProperty().get();
    }

    private NodeTransformation nodeTransformation;

    private NodeTransformation getNodeTransformation() {
        if (nodeTransformation == null) {
            nodeTransformation = new NodeTransformation();
        }

        return nodeTransformation;
    }

    private boolean hasTransforms() {
        return (nodeTransformation != null)
                && nodeTransformation.hasTransforms();
    }

    // for tests only
    Transform getCurrentLocalToSceneTransformState() {
        if (nodeTransformation == null ||
                nodeTransformation.localToSceneTransform == null) {
            return null;
        }

        return nodeTransformation.localToSceneTransform.transform;
    }

    private static final double DEFAULT_TRANSLATE_X = 0;
    private static final double DEFAULT_TRANSLATE_Y = 0;
    private static final double DEFAULT_TRANSLATE_Z = 0;
    private static final double DEFAULT_SCALE_X = 1;
    private static final double DEFAULT_SCALE_Y = 1;
    private static final double DEFAULT_SCALE_Z = 1;
    private static final double DEFAULT_ROTATE = 0;
    private static final Point3D DEFAULT_ROTATION_AXIS = Rotate.Z_AXIS;

    private final class NodeTransformation {
        private DoubleProperty translateX;
        private DoubleProperty translateY;
        private DoubleProperty translateZ;
        private DoubleProperty scaleX;
        private DoubleProperty scaleY;
        private DoubleProperty scaleZ;
        private DoubleProperty rotate;
        private ObjectProperty<Point3D> rotationAxis;
        private ObservableList<Transform> transforms;
        private LazyTransformProperty localToParentTransform;
        private LazyTransformProperty localToSceneTransform;
        private int listenerReasons = 0;
        private InvalidationListener localToSceneInvLstnr;

        private InvalidationListener getLocalToSceneInvalidationListener() {
            if (localToSceneInvLstnr == null) {
                localToSceneInvLstnr = observable -> invalidateLocalToSceneTransform();
            }
            return localToSceneInvLstnr;
        }

        public void incListenerReasons() {
            if (listenerReasons == 0) {
                Node n = Node.this.getParent();
                if (n != null) {
                    n.localToSceneTransformProperty().addListener(
                            getLocalToSceneInvalidationListener());
                }
            }
            listenerReasons++;
        }

        public void decListenerReasons() {
            listenerReasons--;
            if (listenerReasons == 0) {
                Node n = Node.this.getParent();
                if (n != null) {
                    n.localToSceneTransformProperty().removeListener(
                            getLocalToSceneInvalidationListener());
                }
                if (localToSceneTransform != null) {
                    localToSceneTransform.validityUnknown();
                }
            }
        }

        public final Transform getLocalToParentTransform() {
            return localToParentTransformProperty().get();
        }

        public final ReadOnlyObjectProperty<Transform> localToParentTransformProperty() {
            if (localToParentTransform == null) {
                localToParentTransform = new LazyTransformProperty() {
                    @Override
                    protected Transform computeTransform(Transform reuse) {
                        updateLocalToParentTransform();
                        return TransformUtils.immutableTransform(reuse,
                                localToParentTx.getMxx(), localToParentTx.getMxy(), localToParentTx.getMxz(), localToParentTx.getMxt(),
                                localToParentTx.getMyx(), localToParentTx.getMyy(), localToParentTx.getMyz(), localToParentTx.getMyt(),
                                localToParentTx.getMzx(), localToParentTx.getMzy(), localToParentTx.getMzz(), localToParentTx.getMzt());
                    }

                    @Override
                    protected boolean validityKnown() {
                        return true;
                    }

                    @Override
                    protected int computeValidity() {
                        return valid;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "localToParentTransform";
                    }
                };
            }

            return localToParentTransform;
        }

        public void invalidateLocalToParentTransform() {
            if (localToParentTransform != null) {
                localToParentTransform.invalidate();
            }
        }

        public final Transform getLocalToSceneTransform() {
            return localToSceneTransformProperty().get();
        }

        class LocalToSceneTransformProperty extends LazyTransformProperty {
            // need this to track number of listeners
            private List localToSceneListeners;
            // stamps to watch for parent changes when the listeners
            // are not present
            private long stamp, parentStamp;

            @Override
            protected Transform computeTransform(Transform reuse) {
                stamp++;
                updateLocalToParentTransform();

                Node parentNode = Node.this.getParent();
                if (parentNode != null) {
                    final LocalToSceneTransformProperty parentProperty =
                            (LocalToSceneTransformProperty) parentNode.localToSceneTransformProperty();
                    final Transform parentTransform = parentProperty.getInternalValue();

                    parentStamp = parentProperty.stamp;

                    return TransformUtils.immutableTransform(reuse,
                            parentTransform,
                            ((LazyTransformProperty) localToParentTransformProperty()).getInternalValue());
                } else {
                    return TransformUtils.immutableTransform(reuse,
                            ((LazyTransformProperty) localToParentTransformProperty()).getInternalValue());
                }
            }

            @Override
            public Object getBean() {
                return Node.this;
            }

            @Override
            public String getName() {
                return "localToSceneTransform";
            }

            @Override
            protected boolean validityKnown() {
                return listenerReasons > 0;
            }

            @Override
            protected int computeValidity() {
                if (valid != VALIDITY_UNKNOWN) {
                    return valid;
                }

                Node n = (Node) getBean();
                Node parent = n.getParent();

                if (parent != null) {
                    final LocalToSceneTransformProperty parentProperty =
                            (LocalToSceneTransformProperty) parent.localToSceneTransformProperty();

                    if (parentStamp != parentProperty.stamp) {
                        valid = INVALID;
                        return INVALID;
                    }

                    int parentValid = parentProperty.computeValidity();
                    if (parentValid == INVALID) {
                        valid = INVALID;
                    }
                    return parentValid;
                }

                // Validity unknown for root means it is valid
                return VALID;
            }

            @Override
            public void addListener(InvalidationListener listener) {
                incListenerReasons();
                if (localToSceneListeners == null) {
                    localToSceneListeners = new LinkedList<>();
                }
                localToSceneListeners.add(listener);
                super.addListener(listener);
            }

            @Override
            public void addListener(ChangeListener<? super Transform> listener) {
                incListenerReasons();
                if (localToSceneListeners == null) {
                    localToSceneListeners = new LinkedList<>();
                }
                localToSceneListeners.add(listener);
                super.addListener(listener);
            }

            @Override
            public void removeListener(InvalidationListener listener) {
                if (localToSceneListeners != null &&
                        localToSceneListeners.remove(listener)) {
                    decListenerReasons();
                }
                super.removeListener(listener);
            }

            @Override
            public void removeListener(ChangeListener<? super Transform> listener) {
                if (localToSceneListeners != null &&
                        localToSceneListeners.remove(listener)) {
                    decListenerReasons();
                }
                super.removeListener(listener);
            }
        }

        public final ReadOnlyObjectProperty<Transform> localToSceneTransformProperty() {
            if (localToSceneTransform == null) {
                localToSceneTransform = new LocalToSceneTransformProperty();
            }

            return localToSceneTransform;
        }

        public void invalidateLocalToSceneTransform() {
            if (localToSceneTransform != null) {
                localToSceneTransform.invalidate();
            }
        }

        public double getTranslateX() {
            return (translateX == null) ? DEFAULT_TRANSLATE_X
                                        : translateX.get();
        }

        public final DoubleProperty translateXProperty() {
            if (translateX == null) {
                translateX = new StyleableDoubleProperty(DEFAULT_TRANSLATE_X) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.TRANSLATE_X;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "translateX";
                    }
                };
            }
            return translateX;
        }

        public double getTranslateY() {
            return (translateY == null) ? DEFAULT_TRANSLATE_Y : translateY.get();
        }

        public final DoubleProperty translateYProperty() {
            if (translateY == null) {
                translateY = new StyleableDoubleProperty(DEFAULT_TRANSLATE_Y) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.TRANSLATE_Y;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "translateY";
                    }
                };
            }
            return translateY;
        }

        public double getTranslateZ() {
            return (translateZ == null) ? DEFAULT_TRANSLATE_Z : translateZ.get();
        }

        public final DoubleProperty translateZProperty() {
            if (translateZ == null) {
                translateZ = new StyleableDoubleProperty(DEFAULT_TRANSLATE_Z) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.TRANSLATE_Z;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "translateZ";
                    }
                };
            }
            return translateZ;
        }

        public double getScaleX() {
            return (scaleX == null) ? DEFAULT_SCALE_X : scaleX.get();
        }

        public final DoubleProperty scaleXProperty() {
            if (scaleX == null) {
                scaleX = new StyleableDoubleProperty(DEFAULT_SCALE_X) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.SCALE_X;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "scaleX";
                    }
                };
            }
            return scaleX;
        }

        public double getScaleY() {
            return (scaleY == null) ? DEFAULT_SCALE_Y : scaleY.get();
        }

        public final DoubleProperty scaleYProperty() {
            if (scaleY == null) {
                scaleY = new StyleableDoubleProperty(DEFAULT_SCALE_Y) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.SCALE_Y;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "scaleY";
                    }
                };
            }
            return scaleY;
        }

        public double getScaleZ() {
            return (scaleZ == null) ? DEFAULT_SCALE_Z : scaleZ.get();
        }

        public final DoubleProperty scaleZProperty() {
            if (scaleZ == null) {
                scaleZ = new StyleableDoubleProperty(DEFAULT_SCALE_Z) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.SCALE_Z;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "scaleZ";
                    }
                };
            }
            return scaleZ;
        }

        public double getRotate() {
            return (rotate == null) ? DEFAULT_ROTATE : rotate.get();
        }

        public final DoubleProperty rotateProperty() {
            if (rotate == null) {
                rotate = new StyleableDoubleProperty(DEFAULT_ROTATE) {
                    @Override
                    public void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.ROTATE;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "rotate";
                    }
                };
            }
            return rotate;
        }

        public Point3D getRotationAxis() {
            return (rotationAxis == null) ? DEFAULT_ROTATION_AXIS
                                          : rotationAxis.get();
        }

        public final ObjectProperty<Point3D> rotationAxisProperty() {
            if (rotationAxis == null) {
                rotationAxis = new ObjectPropertyBase<Point3D>(
                                           DEFAULT_ROTATION_AXIS) {
                    @Override
                    protected void invalidated() {
                        NodeHelper.transformsChanged(Node.this);
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "rotationAxis";
                    }
                };
            }
            return rotationAxis;
        }

        public ObservableList<Transform> getTransforms() {
            if (transforms == null) {
                transforms = new TrackableObservableList<>() {
                    @Override
                    protected void onChanged(Change<Transform> c) {
                        while (c.next()) {
                            for (Transform t : c.getRemoved()) {
                                TransformHelper.remove(t, Node.this);
                            }
                            for (Transform t : c.getAddedSubList()) {
                                TransformHelper.add(t, Node.this);
                            }
                        }

                        NodeHelper.transformsChanged(Node.this);
                    }
                };
            }

            return transforms;
        }

        public boolean canSetTranslateX() {
            return (translateX == null) || !translateX.isBound();
        }

        public boolean canSetTranslateY() {
            return (translateY == null) || !translateY.isBound();
        }

        public boolean canSetTranslateZ() {
            return (translateZ == null) || !translateZ.isBound();
        }

        public boolean canSetScaleX() {
            return (scaleX == null) || !scaleX.isBound();
        }

        public boolean canSetScaleY() {
            return (scaleY == null) || !scaleY.isBound();
        }

        public boolean canSetScaleZ() {
            return (scaleZ == null) || !scaleZ.isBound();
        }

        public boolean canSetRotate() {
            return (rotate == null) || !rotate.isBound();
        }

        public boolean hasTransforms() {
            return (transforms != null && !transforms.isEmpty());
        }

        public boolean hasScaleOrRotate() {
            if (scaleX != null && scaleX.get() != DEFAULT_SCALE_X) {
                return true;
            }
            if (scaleY != null && scaleY.get() != DEFAULT_SCALE_Y) {
                return true;
            }
            if (scaleZ != null && scaleZ.get() != DEFAULT_SCALE_Z) {
                return true;
            }
            if (rotate != null && rotate.get() != DEFAULT_ROTATE) {
                return true;
            }
            return false;
        }

    }

    //--------------------------
    //  Private Implementation
    //--------------------------

    /* *************************************************************************
     *                                                                         *
     *                        Event Handler Properties                         *
     *                                                                         *
     **************************************************************************/

    private EventHandlerProperties eventHandlerProperties;

    private EventHandlerProperties getEventHandlerProperties() {
        if (eventHandlerProperties == null) {
            eventHandlerProperties =
                    new EventHandlerProperties(
                        getInternalEventDispatcher().getEventHandlerManager(),
                        this);
        }

        return eventHandlerProperties;
    }

    /* *************************************************************************
     *                                                                         *
     *                       Component Orientation Properties                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Node orientation describes the flow of visual data within a node.
     * In the English speaking world, visual data normally flows from
     * left-to-right. In an Arabic or Hebrew world, visual data flows
     * from right-to-left. This is consistent with the reading order
     * of text in both worlds.
     *
     * @defaultValue {@code NodeOrientation.INHERIT}
     * @since JavaFX 8.0
     */
    private ObjectProperty<NodeOrientation> nodeOrientation;
    private EffectiveOrientationProperty effectiveNodeOrientationProperty;

    private static final byte EFFECTIVE_ORIENTATION_LTR = 0;
    private static final byte EFFECTIVE_ORIENTATION_RTL = 1;
    private static final byte EFFECTIVE_ORIENTATION_MASK = 1;
    private static final byte AUTOMATIC_ORIENTATION_LTR = 0;
    private static final byte AUTOMATIC_ORIENTATION_RTL = 2;
    private static final byte AUTOMATIC_ORIENTATION_MASK = 2;

    /**
     * Indicates that the effective node orientation only depends on the explicit value set on this node
     * and on the scene (if the node orientation is inherited), but not on the parent. This flag must only
     * be set with {@link NodeHelper#setInheritOrientationFromScene(Node, boolean)} for scene overlays.
     */
    private static final byte INHERIT_ORIENTATION_FROM_SCENE = 4;

    private byte resolvedNodeOrientation =
            EFFECTIVE_ORIENTATION_LTR | AUTOMATIC_ORIENTATION_LTR;

    public final void setNodeOrientation(NodeOrientation orientation) {
        nodeOrientationProperty().set(orientation);
    }

    public final NodeOrientation getNodeOrientation() {
        return nodeOrientation == null ? NodeOrientation.INHERIT : nodeOrientation.get();
    }

    public final ObjectProperty<NodeOrientation> nodeOrientationProperty() {
        if (nodeOrientation == null) {
            nodeOrientation = new StyleableObjectProperty<NodeOrientation>(NodeOrientation.INHERIT) {
                @Override
                protected void invalidated() {
                    nodeResolvedOrientationInvalidated();
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "nodeOrientation";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    //TODO - not supported
                    throw new UnsupportedOperationException("Not supported yet.");
                }

            };
        }
        return nodeOrientation;
    }

    public final NodeOrientation getEffectiveNodeOrientation() {
        return (getEffectiveOrientation(resolvedNodeOrientation)
                    == EFFECTIVE_ORIENTATION_LTR)
                       ? NodeOrientation.LEFT_TO_RIGHT
                       : NodeOrientation.RIGHT_TO_LEFT;
    }

    /**
     * The effective orientation of a node resolves the inheritance of
     * node orientation, returning either left-to-right or right-to-left.
     * @return the node orientation for this {@code Node}
     * @since JavaFX 8.0
     */
    public final ReadOnlyObjectProperty<NodeOrientation>
            effectiveNodeOrientationProperty() {
        if (effectiveNodeOrientationProperty == null) {
            effectiveNodeOrientationProperty =
                    new EffectiveOrientationProperty();
        }

        return effectiveNodeOrientationProperty;
    }

    /**
     * Determines whether a node should be mirrored when node orientation
     * is right-to-left.
     * <p>
     * When a node is mirrored, the origin is automatically moved to the
     * top right corner causing the node to layout children and draw from
     * right to left using a mirroring transformation.  Some nodes may wish
     * to draw from right to left without using a transformation.  These
     * nodes will answer {@code false} and implement right-to-left
     * orientation without using the automatic transformation.
     * </p>
     * @return true if this {@code Node} should be mirrored
     * @since JavaFX 8.0
     */
    public boolean usesMirroring() {
        return true;
    }

    final void parentResolvedOrientationInvalidated() {
        if (getNodeOrientation() == NodeOrientation.INHERIT) {
            nodeResolvedOrientationInvalidated();
        } else {
            // mirroring changed
            NodeHelper.transformsChanged(this);
        }
    }

    final void nodeResolvedOrientationInvalidated() {
        final byte oldResolvedNodeOrientation =
                resolvedNodeOrientation;

        resolvedNodeOrientation =
                (byte) (calcEffectiveNodeOrientation()
                            | calcAutomaticNodeOrientation());

        if (isInheritOrientationFromScene(oldResolvedNodeOrientation)) {
            resolvedNodeOrientation |= INHERIT_ORIENTATION_FROM_SCENE;
        }

        if ((effectiveNodeOrientationProperty != null)
                && (getEffectiveOrientation(resolvedNodeOrientation)
                        != getEffectiveOrientation(
                               oldResolvedNodeOrientation))) {
            effectiveNodeOrientationProperty.invalidate();
        }

        // mirroring changed
        NodeHelper.transformsChanged(this);

        if (resolvedNodeOrientation != oldResolvedNodeOrientation) {
            nodeResolvedOrientationChanged();
        }
    }

    void nodeResolvedOrientationChanged() {
        // overriden in Parent
    }

    private Node getMirroringOrientationParent() {
        if (isInheritOrientationFromScene(resolvedNodeOrientation)) {
            return null;
        }

        Node parentValue = getParent();
        while (parentValue != null) {
            if (parentValue.usesMirroring()) {
                return parentValue;
            }
            parentValue = parentValue.getParent();
        }

        final Node subSceneValue = getSubScene();
        if (subSceneValue != null) {
            return subSceneValue;
        }

        return null;
    }

    private Node getOrientationParent() {
        if (isInheritOrientationFromScene(resolvedNodeOrientation)) {
            return null;
        }

        final Node parentValue = getParent();
        if (parentValue != null) {
            return parentValue;
        }

        final Node subSceneValue = getSubScene();
        if (subSceneValue != null) {
            return subSceneValue;
        }

        return null;
    }

    private byte calcEffectiveNodeOrientation() {
        final NodeOrientation nodeOrientationValue = getNodeOrientation();
        if (nodeOrientationValue != NodeOrientation.INHERIT) {
            return (nodeOrientationValue == NodeOrientation.LEFT_TO_RIGHT)
                       ? EFFECTIVE_ORIENTATION_LTR
                       : EFFECTIVE_ORIENTATION_RTL;
        }

        final Node parentValue = getOrientationParent();
        if (parentValue != null) {
            return getEffectiveOrientation(parentValue.resolvedNodeOrientation);
        }

        final Scene sceneValue = getScene();
        if (sceneValue != null) {
            return (sceneValue.getEffectiveNodeOrientation()
                        == NodeOrientation.LEFT_TO_RIGHT)
                           ? EFFECTIVE_ORIENTATION_LTR
                           : EFFECTIVE_ORIENTATION_RTL;
        }

        return EFFECTIVE_ORIENTATION_LTR;
    }

    private byte calcAutomaticNodeOrientation() {
        if (!usesMirroring()) {
            return AUTOMATIC_ORIENTATION_LTR;
        }

        final NodeOrientation nodeOrientationValue = getNodeOrientation();
        if (nodeOrientationValue != NodeOrientation.INHERIT) {
            return (nodeOrientationValue == NodeOrientation.LEFT_TO_RIGHT)
                       ? AUTOMATIC_ORIENTATION_LTR
                       : AUTOMATIC_ORIENTATION_RTL;
        }

        final Node parentValue = getMirroringOrientationParent();
        if (parentValue != null) {
            // automatic node orientation is inherited
            return getAutomaticOrientation(parentValue.resolvedNodeOrientation);
        }

        final Scene sceneValue = getScene();
        if (sceneValue != null) {
            return (sceneValue.getEffectiveNodeOrientation()
                        == NodeOrientation.LEFT_TO_RIGHT)
                           ? AUTOMATIC_ORIENTATION_LTR
                           : AUTOMATIC_ORIENTATION_RTL;
        }

        return AUTOMATIC_ORIENTATION_LTR;
    }

    // Return true if the node needs to be mirrored.
    // A node has mirroring if the orientation differs from the parent
    // package private for testing
    final boolean hasMirroring() {
        final Node parentValue = getOrientationParent();

        final byte thisOrientation =
                getAutomaticOrientation(resolvedNodeOrientation);
        final byte parentOrientation =
                (parentValue != null)
                    ? getAutomaticOrientation(
                          parentValue.resolvedNodeOrientation)
                    : AUTOMATIC_ORIENTATION_LTR;

        return thisOrientation != parentOrientation;
    }

    private static byte getEffectiveOrientation(
            final byte resolvedNodeOrientation) {
        return (byte) (resolvedNodeOrientation & EFFECTIVE_ORIENTATION_MASK);
    }

    private static byte getAutomaticOrientation(
            final byte resolvedNodeOrientation) {
        return (byte) (resolvedNodeOrientation & AUTOMATIC_ORIENTATION_MASK);
    }

    private static boolean isInheritOrientationFromScene(byte resolvedNodeOrientation) {
        return (resolvedNodeOrientation & INHERIT_ORIENTATION_FROM_SCENE) != 0;
    }

    private void setInheritOrientationFromScene(boolean value) {
        if (value) {
            resolvedNodeOrientation |= INHERIT_ORIENTATION_FROM_SCENE;
        } else {
            resolvedNodeOrientation &= ~INHERIT_ORIENTATION_FROM_SCENE;
        }
    }

    private final class EffectiveOrientationProperty
            extends ReadOnlyObjectPropertyBase<NodeOrientation> {
        @Override
        public NodeOrientation get() {
            return getEffectiveNodeOrientation();
        }

        @Override
        public Object getBean() {
            return Node.this;
        }

        @Override
        public String getName() {
            return "effectiveNodeOrientation";
        }

        public void invalidate() {
            fireValueChangedEvent();
        }
    }

    /* *************************************************************************
     *                                                                         *
     *                       Misc Seldom Used Properties                       *
     *                                                                         *
     **************************************************************************/

    private MiscProperties miscProperties;

    private MiscProperties getMiscProperties() {
        if (miscProperties == null) {
            miscProperties = new MiscProperties();
        }

        return miscProperties;
    }

    private static final double DEFAULT_VIEW_ORDER = 0;
    private static final boolean DEFAULT_CACHE = false;
    private static final CacheHint DEFAULT_CACHE_HINT = CacheHint.DEFAULT;
    private static final Node DEFAULT_CLIP = null;
    private static final Cursor DEFAULT_CURSOR = null;
    private static final DepthTest DEFAULT_DEPTH_TEST = DepthTest.INHERIT;
    private static final boolean DEFAULT_DISABLE = false;
    private static final Effect DEFAULT_EFFECT = null;
    private static final InputMethodRequests DEFAULT_INPUT_METHOD_REQUESTS =
            null;
    private static final boolean DEFAULT_MOUSE_TRANSPARENT = false;

    private final class MiscProperties {
        private LazyBoundsProperty boundsInParent;
        private LazyBoundsProperty boundsInLocal;
        private BooleanProperty cache;
        private ObjectProperty<CacheHint> cacheHint;
        private ObjectProperty<Node> clip;
        private ObjectProperty<Cursor> cursor;
        private ObjectProperty<DepthTest> depthTest;
        private BooleanProperty disable;
        private ObjectProperty<Effect> effect;
        private ObjectProperty<InputMethodRequests> inputMethodRequests;
        private BooleanProperty mouseTransparent;
        private DoubleProperty viewOrder;
        private TransitionTimerCollection transitionTimers;
        private TransitionDefinitionCollection transitionDefinitions;

        public double getViewOrder() {
            return (viewOrder == null) ? DEFAULT_VIEW_ORDER : viewOrder.get();
        }

        public final DoubleProperty viewOrderProperty() {
            if (viewOrder == null) {
                viewOrder = new StyleableDoubleProperty(DEFAULT_VIEW_ORDER) {
                    @Override
                    public void invalidated() {
                        Parent p = getParent();
                        if (p != null) {
                            // Parent will be responsible to update sorted children list
                            p.markViewOrderChildrenDirty();
                        }
                        NodeHelper.markDirty(Node.this, DirtyBits.NODE_VIEW_ORDER);
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.VIEW_ORDER;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "viewOrder";
                    }
                };
            }
            return viewOrder;
        }

        public final Bounds getBoundsInParent() {
            return boundsInParentProperty().get();
        }

        public final ReadOnlyObjectProperty<Bounds> boundsInParentProperty() {
            if (boundsInParent == null) {
                boundsInParent = new LazyBoundsProperty() {
                    /**
                     * Computes the bounds including the clip, effects, and all
                     * transforms. This function is essentially how to compute
                     * the boundsInParent. Optimizations are made to compute as
                     * little as possible and create as little trash as
                     * possible.
                     */
                    @Override
                    protected Bounds computeBounds() {
                        BaseBounds tempBounds = TempState.getInstance().bounds;
                        tempBounds = getTransformedBounds(
                                             tempBounds,
                                             BaseTransform.IDENTITY_TRANSFORM);
                        return new BoundingBox(tempBounds.getMinX(),
                                               tempBounds.getMinY(),
                                               tempBounds.getMinZ(),
                                               tempBounds.getWidth(),
                                               tempBounds.getHeight(),
                                               tempBounds.getDepth());
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "boundsInParent";
                    }
                };
            }

            return boundsInParent;
        }

        public void invalidateBoundsInParent() {
            if (boundsInParent != null) {
                boundsInParent.invalidate();
            }
        }

        public final Bounds getBoundsInLocal() {
            return boundsInLocalProperty().get();
        }

        public final ReadOnlyObjectProperty<Bounds> boundsInLocalProperty() {
            if (boundsInLocal == null) {
                boundsInLocal = new LazyBoundsProperty() {
                    @Override
                    protected Bounds computeBounds() {
                        BaseBounds tempBounds = TempState.getInstance().bounds;
                        tempBounds = getLocalBounds(
                                             tempBounds,
                                             BaseTransform.IDENTITY_TRANSFORM);
                        return new BoundingBox(tempBounds.getMinX(),
                                               tempBounds.getMinY(),
                                               tempBounds.getMinZ(),
                                               tempBounds.getWidth(),
                                               tempBounds.getHeight(),
                                               tempBounds.getDepth());
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "boundsInLocal";
                    }
                };
            }

            return boundsInLocal;
        }

        public void invalidateBoundsInLocal() {
            if (boundsInLocal != null) {
                boundsInLocal.invalidate();
            }
        }

        public final boolean isCache() {
            return (cache == null) ? DEFAULT_CACHE
                                   : cache.get();
        }

        public final BooleanProperty cacheProperty() {
            if (cache == null) {
                cache = new BooleanPropertyBase(DEFAULT_CACHE) {
                    @Override
                    protected void invalidated() {
                        NodeHelper.markDirty(Node.this, DirtyBits.NODE_CACHE);
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "cache";
                    }
                };
            }
            return cache;
        }

        public final CacheHint getCacheHint() {
            return (cacheHint == null) ? DEFAULT_CACHE_HINT
                                       : cacheHint.get();
        }

        public final ObjectProperty<CacheHint> cacheHintProperty() {
            if (cacheHint == null) {
                cacheHint = new ObjectPropertyBase<CacheHint>(DEFAULT_CACHE_HINT) {
                    @Override
                    protected void invalidated() {
                        NodeHelper.markDirty(Node.this, DirtyBits.NODE_CACHE);
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "cacheHint";
                    }
                };
            }
            return cacheHint;
        }

        public final Node getClip() {
            return (clip == null) ? DEFAULT_CLIP : clip.get();
        }

        public final ObjectProperty<Node> clipProperty() {
            if (clip == null) {
                clip = new ObjectPropertyBase<Node>(DEFAULT_CLIP) {

                    //temp variables used when clip was invalid to rollback to
                    // last value
                    private Node oldClip;

                    @Override
                    protected void invalidated() {
                        final Node newClip = get();
                        if ((newClip != null)
                                && ((newClip.isConnected()
                                           && newClip.clipParent != Node.this)
                                       || wouldCreateCycle(Node.this,
                                                           newClip))) {
                            // Assigning this node to clip is illegal.
                            // Roll back to the previous state and throw an
                            // exception.
                            final String cause =
                                    newClip.isConnected()
                                        && (newClip.clipParent != Node.this)
                                            ? "node already connected"
                                            : "cycle detected";

                            if (isBound()) {
                                unbind();
                                set(oldClip);
                                throw new IllegalArgumentException(
                                        "Node's clip set to incorrect value "
                                            + " through binding"
                                            + " (" + cause + ", node  = "
                                                   + Node.this + ", clip = "
                                                   + clip + ")."
                                            + " Binding has been removed.");
                            } else {
                                set(oldClip);
                                throw new IllegalArgumentException(
                                        "Node's clip set to incorrect value"
                                            + " (" + cause + ", node  = "
                                                   + Node.this + ", clip = "
                                                   + clip + ").");
                            }
                        } else {
                            if (oldClip != null) {
                                oldClip.clipParent = null;
                                oldClip.setScenes(null, null);
                                oldClip.updateTreeVisible(false);
                            }

                            if (newClip != null) {
                                newClip.clipParent = Node.this;
                                newClip.setScenes(getScene(), getSubScene());
                                newClip.updateTreeVisible(true);
                            }

                            NodeHelper.markDirty(Node.this, DirtyBits.NODE_CLIP);

                            // the local bounds have (probably) changed
                            localBoundsChanged();

                            oldClip = newClip;
                        }
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "clip";
                    }
                };
            }
            return clip;
        }

        public final Cursor getCursor() {
            return (cursor == null) ? DEFAULT_CURSOR : cursor.get();
        }

        public final ObjectProperty<Cursor> cursorProperty() {
            if (cursor == null) {
                cursor = new StyleableObjectProperty<Cursor>(DEFAULT_CURSOR) {

                    @Override
                    protected void invalidated() {
                        final Scene sceneValue = getScene();
                        if (sceneValue != null) {
                            sceneValue.markCursorDirty();
                        }
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.CURSOR;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "cursor";
                    }

                };
            }
            return cursor;
        }

        public final DepthTest getDepthTest() {
            return (depthTest == null) ? DEFAULT_DEPTH_TEST
                                       : depthTest.get();
        }

        public final ObjectProperty<DepthTest> depthTestProperty() {
            if (depthTest == null) {
                depthTest = new ObjectPropertyBase<DepthTest>(DEFAULT_DEPTH_TEST) {
                    @Override protected void invalidated() {
                        computeDerivedDepthTest();
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "depthTest";
                    }
                };
            }
            return depthTest;
        }

        public final boolean isDisable() {
            return (disable == null) ? DEFAULT_DISABLE : disable.get();
        }

        public final BooleanProperty disableProperty() {
            if (disable == null) {
                disable = new BooleanPropertyBase(DEFAULT_DISABLE) {
                    @Override
                    protected void invalidated() {
                        updateDisabled();
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "disable";
                    }
                };
            }
            return disable;
        }

        public final Effect getEffect() {
            return (effect == null) ? DEFAULT_EFFECT : effect.get();
        }

        public final ObjectProperty<Effect> effectProperty() {
            if (effect == null) {
                effect = new StyleableObjectProperty<Effect>(DEFAULT_EFFECT) {
                    private Effect oldEffect = null;
                    private int oldBits;

                    private final AbstractNotifyListener effectChangeListener =
                            new AbstractNotifyListener() {

                        @Override
                        public void invalidated(Observable valueModel) {
                            int newBits = ((IntegerProperty) valueModel).get();
                            int changedBits = newBits ^ oldBits;
                            oldBits = newBits;
                            if (EffectDirtyBits.isSet(
                                    changedBits,
                                    EffectDirtyBits.EFFECT_DIRTY)
                                && EffectDirtyBits.isSet(
                                       newBits,
                                       EffectDirtyBits.EFFECT_DIRTY)) {
                                NodeHelper.markDirty(Node.this, DirtyBits.EFFECT_EFFECT);
                            }
                            if (EffectDirtyBits.isSet(
                                    changedBits,
                                    EffectDirtyBits.BOUNDS_CHANGED)) {
                                localBoundsChanged();
                            }
                        }
                    };

                    @Override
                    protected void invalidated() {
                        Effect _effect = get();
                        if (oldEffect != null) {
                            EffectHelper.effectDirtyProperty(oldEffect).removeListener(
                                    effectChangeListener.getWeakListener());
                        }
                        oldEffect = _effect;
                        if (_effect != null) {
                            EffectHelper.effectDirtyProperty(_effect)
                                   .addListener(
                                       effectChangeListener.getWeakListener());
                            if (EffectHelper.isEffectDirty(_effect)) {
                                NodeHelper.markDirty(Node.this, DirtyBits.EFFECT_EFFECT);
                            }
                            oldBits = EffectHelper.effectDirtyProperty(_effect).get();
                        }

                        NodeHelper.markDirty(Node.this, DirtyBits.NODE_EFFECT);
                        // bounds may have changed regardless whether
                        // the dirty flag on effect is set
                        localBoundsChanged();
                    }

                    @Override
                    public CssMetaData getCssMetaData() {
                        return StyleableProperties.EFFECT;
                    }

                    @Override
                    public Object getBean() {
                        return Node.this;
                    }

                    @Override
                    public String getName() {
                        return "effect";
                    }
                };
            }
            return effect;
        }

        public final InputMethodRequests getInputMethodRequests() {
            return (inputMethodRequests == null) ? DEFAULT_INPUT_METHOD_REQUESTS
                                                 : inputMethodRequests.get();
        }

        public ObjectProperty<InputMethodRequests>
                inputMethodRequestsProperty() {
            if (inputMethodRequests == null) {
                inputMethodRequests =
                        new SimpleObjectProperty<>(
                                Node.this,
                                "inputMethodRequests",
                                DEFAULT_INPUT_METHOD_REQUESTS);
            }
            return inputMethodRequests;
        }

        public final boolean isMouseTransparent() {
            return (mouseTransparent == null) ? DEFAULT_MOUSE_TRANSPARENT
                                              : mouseTransparent.get();
        }

        public final BooleanProperty mouseTransparentProperty() {
            if (mouseTransparent == null) {
                mouseTransparent =
                        new SimpleBooleanProperty(
                                Node.this,
                                "mouseTransparent",
                                DEFAULT_MOUSE_TRANSPARENT);
            }
            return mouseTransparent;
        }

        public boolean canSetCursor() {
            return (cursor == null) || !cursor.isBound();
        }

        public boolean canSetEffect() {
            return (effect == null) || !effect.isBound();
        }
    }

    /* *************************************************************************
     *                                                                         *
     *                             Mouse Handling                              *
     *                                                                         *
     **************************************************************************/

    public final void setMouseTransparent(boolean value) {
        mouseTransparentProperty().set(value);
    }

    public final boolean isMouseTransparent() {
        return (miscProperties == null) ? DEFAULT_MOUSE_TRANSPARENT
                                        : miscProperties.isMouseTransparent();
    }

    /**
     * If {@code true}, this node (together with all its children) is completely
     * transparent to mouse events. When choosing target for mouse event, nodes
     * with {@code mouseTransparent} set to {@code true} and their subtrees
     * won't be taken into account.
     * @return is this {@code Node} (together with all its children) is completely
     * transparent to mouse events.
     */
    public final BooleanProperty mouseTransparentProperty() {
        return getMiscProperties().mouseTransparentProperty();
    }

    /**
     * Whether or not this {@code Node} is being hovered over. Typically this is
     * due to the mouse being over the node, though it could be due to a pen
     * hovering on a graphics tablet or other form of input.
     *
     * <p>Note that current implementation of hover relies on mouse enter and
     * exit events to determine whether this Node is in the hover state; this
     * means that this feature is currently supported only on systems that
     * have a mouse. Future implementations may provide alternative means of
     * supporting hover.
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper hover;

    protected final void setHover(boolean value) {
        hoverPropertyImpl().set(value);
    }

    public final boolean isHover() {
        return hover == null ? false : hover.get();
    }

    public final ReadOnlyBooleanProperty hoverProperty() {
        return hoverPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper hoverPropertyImpl() {
        if (hover == null) {
            hover = new ReadOnlyBooleanWrapper() {

                @Override
                protected void invalidated() {
                    PlatformLogger logger = Logging.getInputLogger();
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(this + " hover=" + get());
                    }
                    pseudoClassStateChanged(HOVER_PSEUDOCLASS_STATE, get());
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "hover";
                }
            };
        }
        return hover;
    }

    /**
     * Whether or not the {@code Node} is pressed. Typically this is true when
     * the primary mouse button is down, though subclasses may define other
     * mouse button state or key state to cause the node to be "pressed".
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper pressed;

    protected final void setPressed(boolean value) {
        pressedPropertyImpl().set(value);
    }

    public final boolean isPressed() {
        return pressed == null ? false : pressed.get();
    }

    public final ReadOnlyBooleanProperty pressedProperty() {
        return pressedPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper pressedPropertyImpl() {
        if (pressed == null) {
            pressed = new ReadOnlyBooleanWrapper() {

                @Override
                protected void invalidated() {
                    PlatformLogger logger = Logging.getInputLogger();
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(this + " pressed=" + get());
                    }
                    pseudoClassStateChanged(PRESSED_PSEUDOCLASS_STATE, get());
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "pressed";
                }
            };
        }
        return pressed;
    }

    public final void setOnContextMenuRequested(
            EventHandler<? super ContextMenuEvent> value) {
        onContextMenuRequestedProperty().set(value);
    }

    public final EventHandler<? super ContextMenuEvent> getOnContextMenuRequested() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.onContextMenuRequested();
    }

    /**
     * Defines a function to be called when a context menu
     * has been requested on this {@code Node}.
     * @return the event handler that is called when a context menu has been
     * requested on this {@code Node}
     * @since JavaFX 2.1
     */
    public final ObjectProperty<EventHandler<? super ContextMenuEvent>>
            onContextMenuRequestedProperty() {
        return getEventHandlerProperties().onContextMenuRequestedProperty();
    }

    public final void setOnMouseClicked(
            EventHandler<? super MouseEvent> value) {
        onMouseClickedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseClicked() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseClicked();
    }

    /**
     * Defines a function to be called when a mouse button has been clicked
     * (pressed and released) on this {@code Node}.
     * @return the event handler that is called when a mouse button has been
     * clicked (pressed and released) on this {@code Node}
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseClickedProperty() {
        return getEventHandlerProperties().onMouseClickedProperty();
    }

    public final void setOnMouseDragged(
            EventHandler<? super MouseEvent> value) {
        onMouseDraggedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseDragged() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseDragged();
    }

    /**
     * Defines a function to be called when a mouse button is pressed
     * on this {@code Node} and then dragged.
     * @return the event handler that is called when a mouse button is pressed
     * on this {@code Node} and then dragged
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseDraggedProperty() {
        return getEventHandlerProperties().onMouseDraggedProperty();
    }

    public final void setOnMouseEntered(
            EventHandler<? super MouseEvent> value) {
        onMouseEnteredProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseEntered() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseEntered();
    }

    /**
     * Defines a function to be called when the mouse enters this {@code Node}.
     * @return the event handler that is called when a mouse enters this
     * {@code Node}
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseEnteredProperty() {
        return getEventHandlerProperties().onMouseEnteredProperty();
    }

    public final void setOnMouseExited(
            EventHandler<? super MouseEvent> value) {
        onMouseExitedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseExited() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseExited();
    }

    /**
     * Defines a function to be called when the mouse exits this {@code Node}.
     * @return the event handler that is called when a mouse exits this
     * {@code Node}
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseExitedProperty() {
        return getEventHandlerProperties().onMouseExitedProperty();
    }

    public final void setOnMouseMoved(
            EventHandler<? super MouseEvent> value) {
        onMouseMovedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseMoved() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseMoved();
    }

    /**
     * Defines a function to be called when mouse cursor moves within
     * this {@code Node} but no buttons have been pushed.
     * @return the event handler that is called when a mouse cursor moves
     * within this {@code Node} but no buttons have been pushed
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseMovedProperty() {
        return getEventHandlerProperties().onMouseMovedProperty();
    }

    public final void setOnMousePressed(
            EventHandler<? super MouseEvent> value) {
        onMousePressedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMousePressed() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMousePressed();
    }

    /**
     * Defines a function to be called when a mouse button
     * has been pressed on this {@code Node}.
     * @return the event handler that is called when a mouse button has been
     * pressed on this {@code Node}
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMousePressedProperty() {
        return getEventHandlerProperties().onMousePressedProperty();
    }

    public final void setOnMouseReleased(
            EventHandler<? super MouseEvent> value) {
        onMouseReleasedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseReleased() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseReleased();
    }

    /**
     * Defines a function to be called when a mouse button
     * has been released on this {@code Node}.
     * @return the event handler that is called when a mouse button has been
     * released on this {@code Node}
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseReleasedProperty() {
        return getEventHandlerProperties().onMouseReleasedProperty();
    }

    public final void setOnDragDetected(
            EventHandler<? super MouseEvent> value) {
        onDragDetectedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnDragDetected() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnDragDetected();
    }

    /**
     * Defines a function to be called when drag gesture has been
     * detected. This is the right place to start drag and drop operation.
     * @return the event handler that is called when drag gesture has been
     * detected
     */
    public final ObjectProperty<EventHandler<? super MouseEvent>>
            onDragDetectedProperty() {
        return getEventHandlerProperties().onDragDetectedProperty();
    }

    public final void setOnMouseDragOver(
            EventHandler<? super MouseDragEvent> value) {
        onMouseDragOverProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragOver() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseDragOver();
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * progresses within this {@code Node}.
     * @return the event handler that is called when a full press-drag-release
     * gesture progresses within this {@code Node}
     * @since JavaFX 2.1
     */
    public final ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragOverProperty() {
        return getEventHandlerProperties().onMouseDragOverProperty();
    }

    public final void setOnMouseDragReleased(
            EventHandler<? super MouseDragEvent> value) {
        onMouseDragReleasedProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragReleased() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseDragReleased();
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * ends (by releasing mouse button) within this {@code Node}.
     * @return the event handler that is called when a full press-drag-release
     * gesture ends (by releasing mouse button) within this {@code Node}
     * @since JavaFX 2.1
     */
    public final ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragReleasedProperty() {
        return getEventHandlerProperties().onMouseDragReleasedProperty();
    }

    public final void setOnMouseDragEntered(
            EventHandler<? super MouseDragEvent> value) {
        onMouseDragEnteredProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragEntered() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseDragEntered();
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * enters this {@code Node}.
     * @return the event handler that is called when a full press-drag-release
     * gesture enters this {@code Node}
     * @since JavaFX 2.1
     */
    public final ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragEnteredProperty() {
        return getEventHandlerProperties().onMouseDragEnteredProperty();
    }

    public final void setOnMouseDragExited(
            EventHandler<? super MouseDragEvent> value) {
        onMouseDragExitedProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragExited() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnMouseDragExited();
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * leaves this {@code Node}.
     * @return the event handler that is called when a full press-drag-release
     * gesture leaves this {@code Node}
     * @since JavaFX 2.1
     */
    public final ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragExitedProperty() {
        return getEventHandlerProperties().onMouseDragExitedProperty();
    }

    public final void setOnMouseDragDone(EventHandler<? super MouseDragEvent> value) {
        onMouseDragDoneProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragDone() {
        return (eventHandlerProperties == null) ? null : eventHandlerProperties.getOnMouseDragDone();
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture ends with this node as its source.
     *
     * @return the event handler that is called when a full press-drag-release finishes
     * @see MouseDragEvent#MOUSE_DRAG_DONE
     * @since 26
     */
    public final ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragDoneProperty() {
        return getEventHandlerProperties().onMouseDragDoneProperty();
    }


    /* *************************************************************************
     *                                                                         *
     *                           Gestures Handling                             *
     *                                                                         *
     **************************************************************************/

    public final void setOnScrollStarted(
            EventHandler<? super ScrollEvent> value) {
        onScrollStartedProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScrollStarted() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnScrollStarted();
    }

    /**
     * Defines a function to be called when a scrolling gesture is detected.
     * @return the event handler that is called when a scrolling gesture is
     * detected
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super ScrollEvent>>
            onScrollStartedProperty() {
        return getEventHandlerProperties().onScrollStartedProperty();
    }

    public final void setOnScroll(
            EventHandler<? super ScrollEvent> value) {
        onScrollProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScroll() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnScroll();
    }

    /**
     * Defines a function to be called when user performs a scrolling action.
     * @return the event handler that is called when user performs a scrolling
     * action
     */
    public final ObjectProperty<EventHandler<? super ScrollEvent>>
            onScrollProperty() {
        return getEventHandlerProperties().onScrollProperty();
    }

    public final void setOnScrollFinished(
            EventHandler<? super ScrollEvent> value) {
        onScrollFinishedProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScrollFinished() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnScrollFinished();
    }

    /**
     * Defines a function to be called when a scrolling gesture ends.
     * @return the event handler that is called when a scrolling gesture ends
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super ScrollEvent>>
            onScrollFinishedProperty() {
        return getEventHandlerProperties().onScrollFinishedProperty();
    }

    public final void setOnRotationStarted(
            EventHandler<? super RotateEvent> value) {
        onRotationStartedProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotationStarted() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnRotationStarted();
    }

    /**
     * Defines a function to be called when a rotation gesture is detected.
     * @return the event handler that is called when a rotation gesture is
     * detected
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super RotateEvent>>
            onRotationStartedProperty() {
        return getEventHandlerProperties().onRotationStartedProperty();
    }

    public final void setOnRotate(
            EventHandler<? super RotateEvent> value) {
        onRotateProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotate() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnRotate();
    }

    /**
     * Defines a function to be called when user performs a rotation action.
     * @return the event handler that is called when user performs a rotation
     * action
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super RotateEvent>>
            onRotateProperty() {
        return getEventHandlerProperties().onRotateProperty();
    }

    public final void setOnRotationFinished(
            EventHandler<? super RotateEvent> value) {
        onRotationFinishedProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotationFinished() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnRotationFinished();
    }

    /**
     * Defines a function to be called when a rotation gesture ends.
     * @return the event handler that is called when a rotation gesture ends
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super RotateEvent>>
            onRotationFinishedProperty() {
        return getEventHandlerProperties().onRotationFinishedProperty();
    }

    public final void setOnZoomStarted(
            EventHandler<? super ZoomEvent> value) {
        onZoomStartedProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoomStarted() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnZoomStarted();
    }

    /**
     * Defines a function to be called when a zooming gesture is detected.
     * @return the event handler that is called when a zooming gesture is
     * detected
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super ZoomEvent>>
            onZoomStartedProperty() {
        return getEventHandlerProperties().onZoomStartedProperty();
    }

    public final void setOnZoom(
            EventHandler<? super ZoomEvent> value) {
        onZoomProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoom() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnZoom();
    }

    /**
     * Defines a function to be called when user performs a zooming action.
     * @return the event handler that is called when user performs a zooming
     * action
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super ZoomEvent>>
            onZoomProperty() {
        return getEventHandlerProperties().onZoomProperty();
    }

    public final void setOnZoomFinished(
            EventHandler<? super ZoomEvent> value) {
        onZoomFinishedProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoomFinished() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnZoomFinished();
    }

    /**
     * Defines a function to be called when a zooming gesture ends.
     * @return the event handler that is called when a zooming gesture ends
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super ZoomEvent>>
            onZoomFinishedProperty() {
        return getEventHandlerProperties().onZoomFinishedProperty();
    }

    public final void setOnSwipeUp(
            EventHandler<? super SwipeEvent> value) {
        onSwipeUpProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeUp() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnSwipeUp();
    }

    /**
     * Defines a function to be called when an upward swipe gesture
     * centered over this node happens.
     * @return the event handler that is called when an upward swipe gesture
     * centered over this node happens
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super SwipeEvent>>
            onSwipeUpProperty() {
        return getEventHandlerProperties().onSwipeUpProperty();
    }

    public final void setOnSwipeDown(
            EventHandler<? super SwipeEvent> value) {
        onSwipeDownProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeDown() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnSwipeDown();
    }

    /**
     * Defines a function to be called when a downward swipe gesture
     * centered over this node happens.
     * @return the event handler that is called when a downward swipe gesture
     * centered over this node happens
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super SwipeEvent>>
            onSwipeDownProperty() {
        return getEventHandlerProperties().onSwipeDownProperty();
    }

    public final void setOnSwipeLeft(
            EventHandler<? super SwipeEvent> value) {
        onSwipeLeftProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeLeft() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnSwipeLeft();
    }

    /**
     * Defines a function to be called when a leftward swipe gesture
     * centered over this node happens.
     * @return the event handler that is called when a leftward swipe gesture
     * centered over this node happens
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super SwipeEvent>>
            onSwipeLeftProperty() {
        return getEventHandlerProperties().onSwipeLeftProperty();
    }

    public final void setOnSwipeRight(
            EventHandler<? super SwipeEvent> value) {
        onSwipeRightProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeRight() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnSwipeRight();
    }

    /**
     * Defines a function to be called when an rightward swipe gesture
     * centered over this node happens.
     * @return the event handler that is called when an rightward swipe gesture
     * centered over this node happens
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super SwipeEvent>>
            onSwipeRightProperty() {
        return getEventHandlerProperties().onSwipeRightProperty();
    }


    /* *************************************************************************
     *                                                                         *
     *                             Touch Handling                              *
     *                                                                         *
     **************************************************************************/

    public final void setOnTouchPressed(
            EventHandler<? super TouchEvent> value) {
        onTouchPressedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchPressed() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnTouchPressed();
    }

    /**
     * Defines a function to be called when a new touch point is pressed.
     * @return the event handler that is called when a new touch point is pressed
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super TouchEvent>>
            onTouchPressedProperty() {
        return getEventHandlerProperties().onTouchPressedProperty();
    }

    public final void setOnTouchMoved(
            EventHandler<? super TouchEvent> value) {
        onTouchMovedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchMoved() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnTouchMoved();
    }

    /**
     * Defines a function to be called when a touch point is moved.
     * @return the event handler that is called when a touch point is moved
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super TouchEvent>>
            onTouchMovedProperty() {
        return getEventHandlerProperties().onTouchMovedProperty();
    }

    public final void setOnTouchReleased(
            EventHandler<? super TouchEvent> value) {
        onTouchReleasedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchReleased() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnTouchReleased();
    }

    /**
     * Defines a function to be called when a touch point is released.
     * @return the event handler that is called when a touch point is released
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super TouchEvent>>
            onTouchReleasedProperty() {
        return getEventHandlerProperties().onTouchReleasedProperty();
    }

    public final void setOnTouchStationary(
            EventHandler<? super TouchEvent> value) {
        onTouchStationaryProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchStationary() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnTouchStationary();
    }

    /**
     * Defines a function to be called when a touch point stays pressed and
     * still.
     * @return the event handler that is called when a touch point stays pressed
     * and still
     * @since JavaFX 2.2
     */
    public final ObjectProperty<EventHandler<? super TouchEvent>>
            onTouchStationaryProperty() {
        return getEventHandlerProperties().onTouchStationaryProperty();
    }

    /* *************************************************************************
     *                                                                         *
     *                           Keyboard Handling                             *
     *                                                                         *
     **************************************************************************/

    public final void setOnKeyPressed(
            EventHandler<? super KeyEvent> value) {
        onKeyPressedProperty().set(value);
    }

    public final EventHandler<? super KeyEvent> getOnKeyPressed() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnKeyPressed();
    }

    /**
     * Defines a function to be called when this {@code Node} or its child
     * {@code Node} has input focus and a key has been pressed. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     * @return the event handler that is called when this {@code Node} or its
     * child {@code Node} has input focus and a key has been pressed
     */
    public final ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyPressedProperty() {
        return getEventHandlerProperties().onKeyPressedProperty();
    }

    public final void setOnKeyReleased(
            EventHandler<? super KeyEvent> value) {
        onKeyReleasedProperty().set(value);
    }

    public final EventHandler<? super KeyEvent> getOnKeyReleased() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnKeyReleased();
    }

    /**
     * Defines a function to be called when this {@code Node} or its child
     * {@code Node} has input focus and a key has been released. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     * @return the event handler that is called when this {@code Node} or its
     * child {@code Node} has input focus and a key has been released
     */
    public final ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyReleasedProperty() {
        return getEventHandlerProperties().onKeyReleasedProperty();
    }

    public final void setOnKeyTyped(
            EventHandler<? super KeyEvent> value) {
        onKeyTypedProperty().set(value);
    }

    public final EventHandler<? super KeyEvent> getOnKeyTyped() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnKeyTyped();
    }

    /**
     * Defines a function to be called when this {@code Node} or its child
     * {@code Node} has input focus and a key has been typed. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     * @return the event handler that is called when this {@code Node} or its
     * child {@code Node} has input focus and a key has been typed
     */
    public final ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyTypedProperty() {
        return getEventHandlerProperties().onKeyTypedProperty();
    }

    /* *************************************************************************
     *                                                                         *
     *                           Input Method Handling                         *
     *                                                                         *
     **************************************************************************/

    public final void setOnInputMethodTextChanged(
            EventHandler<? super InputMethodEvent> value) {
        onInputMethodTextChangedProperty().set(value);
    }

    public final EventHandler<? super InputMethodEvent>
            getOnInputMethodTextChanged() {
        return (eventHandlerProperties == null)
                ? null : eventHandlerProperties.getOnInputMethodTextChanged();
    }

    /**
     * Defines a function to be called when this {@code Node}
     * has input focus and the input method text has changed.  If this
     * function is not defined in this {@code Node}, then it
     * receives the result string of the input method composition as a
     * series of {@code onKeyTyped} function calls.
     * <p>
     * When the {@code Node} loses the input focus, the JavaFX runtime
     * automatically commits the existing composed text if any.
     * </p>
     * @return the event handler that is called when this {@code Node} has input
     * focus and the input method text has changed
     */
    public final ObjectProperty<EventHandler<? super InputMethodEvent>>
            onInputMethodTextChangedProperty() {
        return getEventHandlerProperties().onInputMethodTextChangedProperty();
    }

    public final void setInputMethodRequests(InputMethodRequests value) {
        inputMethodRequestsProperty().set(value);
    }

    public final InputMethodRequests getInputMethodRequests() {
        return (miscProperties == null)
                       ? DEFAULT_INPUT_METHOD_REQUESTS
                       : miscProperties.getInputMethodRequests();
    }

    /**
     * Property holding InputMethodRequests.
     *
     * @return InputMethodRequestsProperty
     */
    public final ObjectProperty<InputMethodRequests> inputMethodRequestsProperty() {
        return getMiscProperties().inputMethodRequestsProperty();
    }

    /* *************************************************************************
     *                                                                         *
     *                             Focus Traversal                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Special boolean property which allows for atomic focus change.
     * Focus change means defocusing the old focus owner and focusing a new
     * one. With a usual property, defocusing the old node fires the value
     * changed event and user code can react with something that breaks
     * focusability of the new node, or even remove the new node from the scene.
     * This leads to various error states. This property allows for setting
     * the state without firing the event. The focus change first sets both
     * properties and then fires both events. This makes the focus change look
     * like an atomic operation - when the old node is notified to loose focus,
     * the new node is already focused.
     */
    abstract class FocusPropertyBase extends ReadOnlyBooleanPropertyBase {
        private boolean value;
        private boolean lastNotifiedValue;

        protected abstract PseudoClass getPseudoClass();

        @Override
        public Object getBean() {
            return Node.this;
        }

        @Override
        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }

        protected boolean notifyListeners() {
            if (lastNotifiedValue == value) {
                return false;
            }

            lastNotifiedValue = value;
            pseudoClassStateChanged(getPseudoClass(), value);

            PlatformLogger logger = Logging.getFocusLogger();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this + " " + getName() + "=" + get());
            }

            fireValueChangedEvent();
            return true;
        }
    }

    /**
     * Sets the value of the {@link #focused} and {@link #focusVisible} properties
     * without firing change events. The value of {@link #focusWithin} is set
     * by the implementation of {@link #focused}.
     */
    final void setFocusQuietly(boolean focused, boolean focusVisible) {
        this.focused.set(focused);
        this.focusVisible.set(focused && focusVisible);
    }

    /**
     * Fires change notifications if the value of {@link #focused}, {@link #focusVisible}
     * or {@link #focusWithin} has changed. Change notifications for {@link #focusWithin}
     * are fired on the current node and on all of its parents, if necessary.
     */
    final void notifyFocusListeners() {
        focused.notifyListeners();
        focusVisible.notifyListeners();

        Node node = this;
        do {
            node.focusWithin.notifyListeners();
            node = node.getParent();
        } while (node != null);
    }

    /**
     * Called when the current node was removed from or added to the scene graph.
     * If the current node has the focusWithin bit, we also need to clear and set the focusWithin bits of this
     * node's old and new parents. Note that a scene graph can have more than a single focused node, for example
     * when a PopupWindow is used to present a branch of the scene graph. Since we need to preserve multi-level
     * focus, we need to adjust the focus-within count on all parents of the node.
     */
    private void updateParentsFocusWithin(Node oldParent, Node newParent) {
        if (!focusWithin.get()) {
            return;
        }

        Node node = oldParent;
        while (node != null) {
            node.focusWithin.adjust(-focusWithin.count);
            node = node.getParent();
        }

        node = newParent;
        while (node != null) {
            node.focusWithin.adjust(focusWithin.count);
            node = node.getParent();
        };

        // Since focus changes are atomic, we only fire change notifications after
        // all changes are committed on all old and new parents.
        if (oldParent != null) {
            oldParent.notifyFocusListeners();
        }

        if (newParent != null) {
            newParent.notifyFocusListeners();
        }
    }

    /**
     * Indicates whether this {@code Node} currently has the input focus.
     * To have the input focus, a node must be the {@code Scene}'s focus
     * owner, and the scene must be in a {@code Stage} that is visible
     * and active. See {@link #requestFocus()} for more information.
     *
     * @see #requestFocus()
     * @defaultValue false
     */
    private final FocusPropertyBase focused = new FocusPropertyBase() {
        @Override
        protected PseudoClass getPseudoClass() {
            return FOCUSED_PSEUDOCLASS_STATE;
        }

        @Override
        public String getName() {
            return "focused";
        }

        @Override
        protected boolean notifyListeners() {
            if (super.notifyListeners()) {
                notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUSED);
                return true;
            }
            return false;
        }

        @Override
        public void set(boolean value) {
            if (get() != value) {
                super.set(value);

                int change = value ? 1 : -1;
                Node node = Node.this;

                do {
                    node.focusWithin.adjust(change);
                    node = node.getParent();
                } while (node != null);
            }
        }
    };

    protected final void setFocused(boolean value) {
        setFocusQuietly(value, false);
        notifyFocusListeners();
    }

    public final boolean isFocused() {
        return focused.get();
    }

    public final ReadOnlyBooleanProperty focusedProperty() {
        return focused;
    }

    /**
     * Indicates whether this {@code Node} should visibly indicate focus.
     * This flag is set when the node acquires input focus via keyboard navigation,
     * and it is cleared when the node loses focus or when {@link #requestFocus()}
     * is called.
     *
     * @defaultValue false
     * @since 19
     */
    final FocusPropertyBase focusVisible = new FocusPropertyBase() {
        @Override
        protected PseudoClass getPseudoClass() {
            return FOCUS_VISIBLE_PSEUDOCLASS_STATE;
        }

        @Override
        public String getName() {
            return "focusVisible";
        }
    };

    public final boolean isFocusVisible() {
        return focusVisible.get();
    }

    public final ReadOnlyBooleanProperty focusVisibleProperty() {
        return focusVisible;
    }

    /**
     * Indicates whether this {@code Node} or any of its descendants currently
     * has the input focus.
     *
     * @defaultValue false
     * @since 19
     */
    private final FocusWithinProperty focusWithin = new FocusWithinProperty();

    private class FocusWithinProperty extends FocusPropertyBase {
        int count;

        @Override
        protected PseudoClass getPseudoClass() {
            return FOCUS_WITHIN_PSEUDOCLASS_STATE;
        }

        @Override
        public String getName() {
            return "focusWithin";
        }

        /**
         * Adjusts the number of focused nodes within this node.
         */
        void adjust(int change) {
            count += change;
            set(count > 0);
        }
    };

    public final boolean isFocusWithin() {
        return focusWithin.get();
    }

    public final ReadOnlyBooleanProperty focusWithinProperty() {
        return focusWithin;
    }

    /**
     * Specifies whether this {@code Node} should be a part of focus traversal
     * cycle. When this property is {@code true} focus can be moved to this
     * {@code Node} and from this {@code Node} using regular focus traversal
     * keys. On a desktop such keys are usually {@code TAB} for moving focus
     * forward and {@code SHIFT+TAB} for moving focus backward.
     *
     * When a {@code Scene} is created, the system gives focus to a
     * {@code Node} whose {@code focusTraversable} variable is true
     * and that is eligible to receive the focus,
     * unless the focus had been set explicitly via a call
     * to {@link #requestFocus()}.
     *
     * @see #requestFocus()
     * @defaultValue false
     */
    private BooleanProperty focusTraversable;

    public final void setFocusTraversable(boolean value) {
        focusTraversableProperty().set(value);
    }
    public final boolean isFocusTraversable() {
        return focusTraversable == null ? false : focusTraversable.get();
    }

    public final BooleanProperty focusTraversableProperty() {
        if (focusTraversable == null) {
            focusTraversable = new StyleableBooleanProperty(false) {

                @Override
                public void invalidated() {
                    Scene _scene = getScene();
                    if (_scene != null) {
                        if (get()) {
                            _scene.initializeInternalEventDispatcher();
                        }
                        focusSetDirty(_scene);
                    }
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.FOCUS_TRAVERSABLE;
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "focusTraversable";
                }
            };
        }
        return focusTraversable;
    }

    /**
     * Called when something has changed on this node that *may* have made the
     * scene's focus dirty. This covers the cases where this node is the focus
     * owner and it may have lost eligibility, or it's traversable and it may
     * have gained eligibility. Note that we do not want to use disabled
     * or treeVisible here, as this function is called from their
     * "on invalidate" triggers, and using them will cause them to be
     * revalidated. The pulse will revalidate everything and make the final
     * determination.
     */
    private void focusSetDirty(Scene s) {
        if (s != null &&
            (this == s.getFocusOwner() || isFocusTraversable())) {
                s.setFocusDirty(true);
        }
    }

    /**
     * Requests that this {@code Node} get the input focus, and that this
     * {@code Node}'s top-level ancestor become the focused window. To be
     * eligible to receive the focus, the node must be part of a scene, it and
     * all of its ancestors must be visible, and it must not be disabled.
     * If this node is eligible, this function will cause it to become this
     * {@code Scene}'s "focus owner". Each scene has at most one focus owner
     * node. The focus owner will not actually have the input focus, however,
     * unless the scene belongs to a {@code Stage} that is both visible
     * and active.
     * <p>This method will clear the {@link #focusVisible} flag.
     */
    public void requestFocus() {
        if (getScene() != null) {
            getScene().requestFocus(this, false);
        }
    }

    /**
     * Requests focus as if by calling {@link #requestFocus()}, and additionally
     * sets the {@link #focusVisible} flag.
     */
    private void requestFocusVisible() {
        if (getScene() != null) {
            getScene().requestFocus(this, true);
        }
    }

    /**
     * Traverses from this node in the direction indicated. Note that this
     * node need not actually have the focus, nor need it be focusTraversable.
     * However, the node must be part of a scene, otherwise this request
     * is ignored.
     */
    final boolean traverse(Direction dir, TraversalMethod method) {
        if (getScene() == null) {
            return false;
        }
        return getScene().traverse(this, dir, method);
    }

    /**
     * Requests to move the focus from this {@code Node} in the specified direction.
     * The {@code Node} serves as a reference point and does not have to be focused or focusable.
     * A successful traversal results in a new {@code Node} being focused.
     * <p>
     * This method is expected to be called in response to a {@code KeyEvent}; therefore the {@code Node}
     * receiving focus will have the {@link #focusVisibleProperty() focusVisible} property set.
     *
     * @param direction the direction of focus traversal, non-null
     * @return {@code true} if traversal was successful
     * @since 24
     */
    public final boolean requestFocusTraversal(TraversalDirection direction) {
        Direction d = Direction.of(direction);
        return traverse(d, TraversalMethod.KEY);
    }

    //--------------------------
    //  Private Implementation
    //--------------------------

     /**
      * Returns a string representation for the object.
      * @return a string representation for the object.
      */
    @Override
    public String toString() {
        String klassName = getClass().getName();
        String simpleName = klassName.substring(klassName.lastIndexOf('.')+1);
        StringBuilder sbuf = new StringBuilder(simpleName);
        boolean hasId = id != null && !"".equals(getId());
        boolean hasStyleClass = !getStyleClass().isEmpty();

        if (!hasId) {
            sbuf.append('@');
            sbuf.append(Integer.toHexString(hashCode()));
        } else {
            sbuf.append("[id=");
            sbuf.append(getId());
            if (!hasStyleClass) sbuf.append("]");
        }
        if (hasStyleClass) {
            if (!hasId) sbuf.append('[');
            else sbuf.append(", ");
            sbuf.append("styleClass=");
            sbuf.append(getStyleClass());
            sbuf.append("]");
        }
        return sbuf.toString();
    }

    private void preprocessMouseEvent(MouseEvent e) {
        final EventType<?> eventType = e.getEventType();
        if (eventType == MouseEvent.MOUSE_PRESSED) {
            for (Node n = this; n != null; n = n.getParent()) {
                n.setPressed(e.isPrimaryButtonDown());
            }
            return;
        }
        if (eventType == MouseEvent.MOUSE_RELEASED) {
            for (Node n = this; n != null; n = n.getParent()) {
                n.setPressed(e.isPrimaryButtonDown());
            }
            return;
        }

        if (e.getTarget() == this) {
            // the mouse event types are translated only when the node uses
            // its internal event dispatcher, so both entered / exited variants
            // are possible here

            if ((eventType == MouseEvent.MOUSE_ENTERED)
                    || (eventType == MouseEvent.MOUSE_ENTERED_TARGET)) {
                setHover(true);
                return;
            }

            if ((eventType == MouseEvent.MOUSE_EXITED)
                    || (eventType == MouseEvent.MOUSE_EXITED_TARGET)) {
                setHover(false);
                return;
            }
        }
    }

    void markDirtyLayoutBranch() {
        Parent p = getParent();
        while (p != null && p.layoutFlag == LayoutFlags.CLEAN) {
            p.setLayoutFlag(LayoutFlags.DIRTY_BRANCH);
            if (p.isSceneRoot()) {
                Toolkit.getToolkit().requestNextPulse();
                if (getSubScene() != null) {
                    getSubScene().setDirtyLayout(p);
                }
            }
            p = p.getParent();
        }

    }

    private boolean isWindowShowing() {
        Scene s = getScene();
        if (s == null) return false;
        Window w = s.getWindow();
        return w != null && w.isShowing();
    }

    final boolean isTreeShowing() {
        return isTreeVisible() && isWindowShowing();
    }

    private void updateTreeVisible(boolean parentChanged) {
        boolean isTreeVisible = isVisible();
        final Node parentNode = getParent() != null ? getParent() :
                    clipParent != null ? clipParent :
                    getSubScene() != null ? getSubScene() : null;
        if (isTreeVisible) {
            isTreeVisible = parentNode == null || parentNode.isTreeVisible();
        }
        // When the parent has changed to visible and we have unsynchronized visibility,
        // we have to synchronize, because the rendering will now pass through the newly-visible parent
        // Otherwise an invisible Node might get rendered
        if (parentChanged && parentNode != null && parentNode.isTreeVisible()
                && isDirty(DirtyBits.NODE_VISIBLE)) {
            addToSceneDirtyList();
        }
        setTreeVisible(isTreeVisible);
    }

    private boolean treeVisible = true;
    private TreeVisibleProperty treeVisibleProperty;

    final void setTreeVisible(boolean value) {
        if (treeVisible != value) {
            treeVisible = value;
            if (!value) {
                // When this node is removed from the scene graph or becomes invisible, we complete
                // all running transitions for this node. This ensures that a node is not affected
                // by a transition when it is no longer useful.
                completeTransitionTimers();
            }
            updateCanReceiveFocus();
            focusSetDirty(getScene());
            if (getClip() != null) {
                getClip().updateTreeVisible(true);
            }
            if (treeVisible && !isDirtyEmpty()) {
                addToSceneDirtyList();
            }
            if (treeVisibleProperty != null) {
                treeVisibleProperty.invalidate();
            }
            if (Node.this instanceof Parent parent) {
                for (Node child : parent.getChildren()) {
                    child.updateTreeVisible(true);
                }
            } else if (Node.this instanceof SubScene subScene) {
                Node subSceneRoot = subScene.getRoot();
                if (subSceneRoot != null) {
                    // SubScene.getRoot() is only null if it's constructor
                    // has not finished.
                    subSceneRoot.setTreeVisible(value && subSceneRoot.isVisible());
                }
            }
        }
    }

    final boolean isTreeVisible() {
        return treeVisibleProperty().get();
    }

    final ReadOnlyBooleanProperty treeVisibleProperty() {
        if (treeVisibleProperty == null) {
            treeVisibleProperty = new TreeVisibleProperty();
        }
        return treeVisibleProperty;
    }

    class TreeVisibleProperty extends ReadOnlyBooleanPropertyBase {

        private boolean valid;

        @Override
        public Object getBean() {
            return Node.this;
        }

        @Override
        public String getName() {
            return "treeVisible";
        }

        protected void invalidate() {
            if (valid) {
                valid = false;
                fireValueChangedEvent();
            }
        }

        @Override
        public boolean get() {
            valid = true;
            return Node.this.treeVisible;
        }

    }

    private boolean canReceiveFocus = false;

    private void setCanReceiveFocus(boolean value) {
        canReceiveFocus = value;
    }

    final boolean isCanReceiveFocus() {
        return canReceiveFocus;
    }

    private void updateCanReceiveFocus() {
        setCanReceiveFocus(getScene() != null
          && !isDisabled()
          && isTreeVisible());
    }

    // for indenting messages based on scene-graph depth
    String indent() {
        String indent = "";
        Parent p = this.getParent();
        while (p != null) {
            indent += "  ";
            p = p.getParent();
        }
        return indent;
    }

    /*
     * Should we underline the mnemonic character?
     */
    private BooleanProperty showMnemonics;

    final void setShowMnemonics(boolean value) {
        showMnemonicsProperty().set(value);
    }

    final boolean isShowMnemonics() {
        return showMnemonics == null ? false : showMnemonics.get();
    }

    final BooleanProperty showMnemonicsProperty() {
        if (showMnemonics == null) {
            showMnemonics = new BooleanPropertyBase(false) {

                @Override
                protected void invalidated() {
                    pseudoClassStateChanged(SHOW_MNEMONICS_PSEUDOCLASS_STATE, get());
                }

                @Override
                public Object getBean() {
                    return Node.this;
                }

                @Override
                public String getName() {
                    return "showMnemonics";
                }
            };
        }
        return showMnemonics;
    }


    /**
     * References a node that is a labelFor this node.
     * Accessible via a NodeAccessor. See Label.labelFor for details.
     */
    private Node labeledBy = null;


    /* *************************************************************************
     *                                                                         *
     *                         Event Dispatch                                  *
     *                                                                         *
     **************************************************************************/

    // PENDING_DOC_REVIEW
    /**
     * Specifies the event dispatcher for this node. The default event
     * dispatcher sends the received events to the registered event handlers and
     * filters. When replacing the value with a new {@code EventDispatcher},
     * the new dispatcher should forward events to the replaced dispatcher
     * to maintain the node's default event handling behavior.
     */
    private ObjectProperty<EventDispatcher> eventDispatcher;

    public final void setEventDispatcher(EventDispatcher value) {
        eventDispatcherProperty().set(value);
    }

    public final EventDispatcher getEventDispatcher() {
        return eventDispatcherProperty().get();
    }

    public final ObjectProperty<EventDispatcher> eventDispatcherProperty() {
        initializeInternalEventDispatcher();
        return eventDispatcher;
    }

    private NodeEventDispatcher internalEventDispatcher;

    @Override
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventHandler(eventType, eventHandler);
    }

    @Override
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher()
                .getEventHandlerManager()
                .removeEventHandler(eventType, eventHandler);
    }

    @Override
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventFilter(eventType, eventFilter);
    }

    @Override
    public final <T extends Event> void removeEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .removeEventFilter(eventType, eventFilter);
    }

    /**
     * Sets the handler to use for this event type. There can only be one such handler
     * specified at a time. This handler is guaranteed to be called as the last, after
     * handlers added using {@link #addEventHandler(javafx.event.EventType, javafx.event.EventHandler)}.
     * This is used for registering the user-defined onFoo event handlers.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type to associate with the given eventHandler
     * @param eventHandler the handler to register, or null to unregister
     * @throws NullPointerException if the event type is null
     */
    protected final <T extends Event> void setEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .setEventHandler(eventType, eventHandler);
    }

    private NodeEventDispatcher getInternalEventDispatcher() {
        initializeInternalEventDispatcher();
        return internalEventDispatcher;
    }

    private void initializeInternalEventDispatcher() {
        if (internalEventDispatcher == null) {
            internalEventDispatcher = createInternalEventDispatcher();
            eventDispatcher = new SimpleObjectProperty<>(
                                          Node.this,
                                          "eventDispatcher",
                                          internalEventDispatcher);
        }
    }

    private NodeEventDispatcher createInternalEventDispatcher() {
        return new NodeEventDispatcher(this);
    }

    /**
     * Event dispatcher for invoking preprocessing of mouse events
     */
    private EventDispatcher preprocessMouseEventDispatcher;

    @Override
    public EventDispatchChain buildEventDispatchChain(
            EventDispatchChain tail) {

        if (preprocessMouseEventDispatcher == null) {
            preprocessMouseEventDispatcher = (event, tail1) -> {
                event = tail1.dispatchEvent(event);
                if (event instanceof MouseEvent) {
                    preprocessMouseEvent((MouseEvent) event);
                }

                return event;
            };
        }

        tail = tail.prepend(preprocessMouseEventDispatcher);

        // prepend all event dispatchers from this node to the root
        Node curNode = this;
        do {
            if (curNode.eventDispatcher != null) {
                final EventDispatcher eventDispatcherValue =
                        curNode.eventDispatcher.get();
                if (eventDispatcherValue != null) {
                    tail = tail.prepend(eventDispatcherValue);
                }
            }
            final Node curParent = curNode.getParent();
            curNode = curParent != null ? curParent : curNode.getSubScene();
        } while (curNode != null);

        if (getScene() != null) {
            // prepend scene's dispatch chain
            tail = getScene().buildEventDispatchChain(tail);
        }

        return tail;
    }

    // PENDING_DOC_REVIEW
    /**
     * Fires the specified event. By default the event will travel through the
     * hierarchy from the stage to this node. Any event filter encountered will
     * be notified and can consume the event. If not consumed by the filters,
     * the event handlers on this node are notified. If these don't consume the
     * event either, the event will travel back the same path it arrived to
     * this node. All event handlers encountered are called and can consume the
     * event.
     * <p>
     * This method must be called on the FX user thread.
     *
     * @param event the event to fire
     */
    public final void fireEvent(Event event) {

        /* Log input events.  We do a coarse filter for at least the FINE
         * level and then granularize from there.
         */
        if (event instanceof InputEvent) {
            PlatformLogger logger = Logging.getInputLogger();
            if (logger.isLoggable(Level.FINE)) {
                EventType eventType = event.getEventType();
                if (eventType == MouseEvent.MOUSE_ENTERED ||
                    eventType == MouseEvent.MOUSE_EXITED) {
                    logger.finer(event.toString());
                } else if (eventType == MouseEvent.MOUSE_MOVED ||
                           eventType == MouseEvent.MOUSE_DRAGGED) {
                    logger.finest(event.toString());
                } else {
                    logger.fine(event.toString());
                }
            }
        }

        Event.fireEvent(this, event);
    }


    /* *************************************************************************
     *                                                                         *
     *                           CSS Transitions                               *
     *                                                                         *
     **************************************************************************/

    /**
     * The list of running transition timers for this node, implemented as a mapping of
     * CSS property names to {@link TransitionTimer} instances.
     */
    private static class TransitionTimerCollection extends HashMap<String, TransitionTimer> {
        TransitionTimerCollection() {
            super(4);
        }

        /**
         * Returns all transition timers for the specified {@code StyleableProperty},
         * including all of its sub-properties.
         *
         * @param property the {@code StyleableProperty}
         * @return a mapping of CSS property names to {@code TransitionTimer} instances
         */
        public Map<String, TransitionTimer> getAll(StyleableProperty<?> property) {
            if (isEmpty()) {
                return Map.of();
            }

            var result = collectTransitionTimers(property, null);
            return result != null ? result : Map.of();
        }

        /**
         * Finds all transition timers for the specified {@code StyleableProperty}, including all
         * of its sub-properties. This method does not allocate a new {@code Map} for the common
         * case where no transitions are found.
         *
         * @param property the {@code StyleableProperty}
         * @param result this argument is an implementation detail, callers must specify {@code null}
         * @return a mapping of CSS property names to {@code TransitionTimer} instances
         */
        private Map<String, TransitionTimer> collectTransitionTimers(
                StyleableProperty<?> property, Map<String, TransitionTimer> result) {
            CssMetaData<? extends Styleable, ?> metadata = property.getCssMetaData();
            String propertyName = metadata.getProperty();
            TransitionTimer timer = get(propertyName);
            if (timer != null) {
                if (result == null) {
                    // We expect the number of concurrent transitions for any given node to be
                    // quite small in most cases, six mappings should be enough.
                    result = HashMap.newHashMap(6);
                }

                result.put(propertyName, timer);
            }

            List<CssMetaData<? extends Styleable, ?>> subMetadata = metadata.getSubProperties();
            if (subMetadata == null) {
                return result;
            }

            for (int i = 0, max = subMetadata.size(); i < max; ++i) {
                result = collectTransitionTimers(property, result);
            }

            return result;
        }
    }

    /**
     * Called by animatable {@link StyleableProperty} implementations in order to register
     * a running {@link TransitionTimer} with this {@code Node}. This allows the node
     * to keep track of running timers that are targeting its properties.
     *
     * @param propertyName the CSS name of the targeted property
     * @param timer the transition timer
     */
    private void addTransitionTimer(String propertyName, TransitionTimer timer) {
        var transitionTimers = miscProperties != null ? miscProperties.transitionTimers : null;
        if (transitionTimers == null) {
            transitionTimers = new TransitionTimerCollection();
            getMiscProperties().transitionTimers = transitionTimers;
        }

        transitionTimers.put(propertyName, timer);
    }

    /**
     * Removes a timer that was previously registered with {@link #addTransitionTimer}.
     * This method is called by animatable {@link StyleableProperty} implementations
     * when their {@link TransitionTimer} has completed.
     *
     * @param propertyName the CSS name of the targeted property
     */
    private void removeTransitionTimer(String propertyName) {
        var transitionTimers = miscProperties != null ? miscProperties.transitionTimers : null;
        if (transitionTimers != null) {
            transitionTimers.remove(propertyName);

            if (transitionTimers.isEmpty()) {
                miscProperties.transitionTimers = null;
            }
        }
    }

    /**
     * Finds the transition timer that targets the specified {@code propertyName}.
     *
     * @param propertyName the CSS name of the targeted property
     * @return the transition timer, or {@code null} if the property is not
     *         targeted by a transition timer
     */
    private TransitionTimer findTransitionTimer(String propertyName) {
        var transitionTimers = miscProperties != null ? miscProperties.transitionTimers : null;
        return transitionTimers != null ? transitionTimers.get(propertyName) : null;
    }

    /**
     * Finds all transition timers that target the specified {@code property}.
     *
     * @param property the targeted property
     * @return a mapping of property names to transition timers, or an empty map if the
     *         property is not targeted by any transition timers
     */
    private Map<String, TransitionTimer> findTransitionTimers(StyleableProperty<?> property) {
        var transitionTimers = miscProperties != null ? miscProperties.transitionTimers : null;
        return transitionTimers != null ? transitionTimers.getAll(property) : Map.of();
    }

    /**
     * Completes all running timers, which skips the rest of their animation and sets
     * the property to the target value.
     */
    // package-private for testing
    void completeTransitionTimers() {
        var transitionTimers = miscProperties != null ? miscProperties.transitionTimers : null;
        if (transitionTimers == null || transitionTimers.isEmpty()) {
            return;
        }

        // Make a copy of the timers, because completing the timers causes them to be removed
        // from the map, which would result in a ConcurrentModificationException.
        for (TransitionTimer timer : List.copyOf(transitionTimers.values())) {
            timer.complete();
        }
    }

    // package-private for testing
    Map<String, TransitionTimer> getTransitionTimers() {
        return miscProperties != null ? miscProperties.transitionTimers : null;
    }

    /**
     * Contains descriptions of the animated transitions that are currently defined for
     * properties of this {@code Node}.
     * <p>
     * All property transitions are implicit, which means they are started automatically by
     * the CSS subsystem when a property value is changed. Explicit property changes, such as
     * by calling {@link Property#setValue(Object)}, do not trigger an animated transition.
     */
    private static class TransitionDefinitionCollection
            extends ArrayList<TransitionDefinition>
            implements StyleableProperty<TransitionDefinition[]> {
        private StyleOrigin origin;

        /**
         * Returns the transition for the property referenced by the specified CSS metadata,
         * or {@code null} if no transition was found. This method does not account for
         * sub-properties of the specified property.
         *
         * @param metadata the CSS metadata of the property
         * @return the {@code TransitionDefinition} specified for the property referenced by the
         *         CSS metadata, {@code null} otherwise
         */
        public TransitionDefinition find(CssMetaData<? extends Styleable, ?> metadata) {
            int size = size();
            if (size == 0) {
                return null;
            }

            // We look for a matching transition in reverse, since multiple transitions might be specified
            // for the same property. In this case, the last transition takes precedence.
            for (int i = size - 1; i >= 0; --i) {
                TransitionDefinition transition = get(i);

                boolean selected = TransitionDefinitionConverter.PROPERTY_ALL.equals(transition.propertyName())
                    || metadata.getProperty().equals(transition.propertyName());

                if (selected) {
                    return transition;
                }
            }

            return null;
        }

        /**
         * Returns all transitions for the property referenced by the specified CSS metadata,
         * including all of its sub-properties.
         *
         * @param metadata the CSS metadata of the property
         * @return a mapping of CSS metadata instances to {@code TransitionDefinition} instances
         */
        public Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> findAll(
                CssMetaData<? extends Styleable, ?> metadata) {
            if (isEmpty()) {
                return Map.of();
            }

            var result = collectTransitions(metadata, null);
            return result != null ? result : Map.of();
        }

        /**
         * Finds all transition definitions for the property referenced by the specified CSS metadata,
         * including all of its sub-properties. This method does not allocate a new {@code Map} for the
         * common case where no transitions are found.
         *
         * @param metadata the CSS metadata of the property
         * @param result this argument is an implementation detail, callers must specify {@code null}
         * @return a mapping of CSS metadata instances to {@code TransitionDefinition} instances
         */
        private Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> collectTransitions(
                CssMetaData<? extends Styleable, ?> metadata,
                Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> result) {
            // We look for a matching transition in reverse, since multiple transitions might be specified
            // for the same property. In this case, the last transition takes precedence.
            for (int max = size(), i = max - 1; i >= 0; --i) {
                TransitionDefinition transition = get(i);

                boolean selected = TransitionDefinitionConverter.PROPERTY_ALL.equals(transition.propertyName())
                        || metadata.getProperty().equals(transition.propertyName());

                if (selected) {
                    if (result == null) {
                        // We expect the number of concurrent transitions for any given node to be
                        // quite small in most cases, six mappings should be enough.
                        result = HashMap.newHashMap(6);
                    }

                    result.put(metadata, transition);
                    break;
                }
            }

            List<CssMetaData<? extends Styleable, ?>> subMetadata = metadata.getSubProperties();
            if (subMetadata == null) {
                return result;
            }

            // We also need to search for transitions defined on all sub-properties of the current property.
            for (int i = 0, max = subMetadata.size(); i < max; ++i) {
                result = collectTransitions(subMetadata.get(i), result);
            }

            return result;
        }

        @Override
        public TransitionDefinition[] getValue() {
            return toArray(TransitionDefinition[]::new);
        }

        @Override
        public void setValue(TransitionDefinition[] value) {
            clear();
            addAll(Arrays.asList(value));
            this.origin = StyleOrigin.USER;
        }

        @Override
        public void applyStyle(StyleOrigin origin, TransitionDefinition[] value) {
            setValue(value);
            this.origin = origin;
        }

        @Override
        public StyleOrigin getStyleOrigin() {
            return origin;
        }

        @Override
        public CssMetaData<? extends Styleable, TransitionDefinition[]> getCssMetaData() {
            return TransitionDefinitionCssMetaData.getInstance();
        }
    }

    // package-private for testing
    List<TransitionDefinition> getTransitionDefinitions() {
        return miscProperties != null ? miscProperties.transitionDefinitions : null;
    }


    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


    /**
     * {@inheritDoc}
     * @return {@code getClass().getName()} without the package name
     * @since JavaFX 8.0
     */
    @Override
    public String getTypeSelector() {

        final Class<?> clazz = getClass();
        final Package pkg = clazz.getPackage();

        // package could be null. not likely, but could be.
        int plen = 0;
        if (pkg != null) {
            plen = pkg.getName().length();
        }

        final int clen = clazz.getName().length();
        final int pos = (0 < plen && plen < clen) ? plen + 1 : 0;

        return clazz.getName().substring(pos);
    }

    /**
     * {@inheritDoc}
     * @return {@code getParent()}
     * @since JavaFX 8.0
     */
    @Override
    public Styleable getStyleableParent() {
        return getParent();
    }


    /**
     * Returns the initial focus traversable state of this node, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * can be overridden by subclasses in instances where focus traversable should
     * initially be true (as the default implementation of this method is to return
     * false).
     *
     * @return the initial focus traversable state for this {@code Node}.
     * @since 9
     */
    protected Boolean getInitialFocusTraversable() {
        return Boolean.FALSE;
    }

    /**
     * Returns the initial cursor state of this node, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * can be overridden by subclasses in instances where the cursor should
     * initially be non-null (as the default implementation of this method is to return
     * null).
     *
     * @return the initial cursor state for this {@code Node}.
     * @since 9
     */
    protected Cursor getInitialCursor() {
        return null;
    }

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      */
     private static class StyleableProperties {

        private static final CssMetaData<Node,Cursor> CURSOR =
            new CssMetaData<>("-fx-cursor", CursorConverter.getInstance()) {

                @Override
                public boolean isSettable(Node node) {
                    return node.miscProperties == null || node.miscProperties.canSetCursor();
                }

                @Override
                public StyleableProperty<Cursor> getStyleableProperty(Node node) {
                    return (StyleableProperty<Cursor>)node.cursorProperty();
                }

                @Override
                public Cursor getInitialValue(Node node) {
                    // Most controls default focusTraversable to true.
                    // Give a way to have them return the correct default value.
                    return node.getInitialCursor();
                }

            };
        private static final CssMetaData<Node,Effect> EFFECT =
            new CssMetaData<>("-fx-effect", EffectConverter.getInstance()) {

                @Override
                public boolean isSettable(Node node) {
                    return node.miscProperties == null || node.miscProperties.canSetEffect();
                }

                @Override
                public StyleableProperty<Effect> getStyleableProperty(Node node) {
                    return (StyleableProperty<Effect>)node.effectProperty();
                }
            };
        private static final CssMetaData<Node,Boolean> FOCUS_TRAVERSABLE =
            new CssMetaData<>("-fx-focus-traversable",
                BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override
                public boolean isSettable(Node node) {
                    return node.focusTraversable == null || !node.focusTraversable.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Node node) {
                    return (StyleableProperty<Boolean>)node.focusTraversableProperty();
                }

                @Override
                public Boolean getInitialValue(Node node) {
                    // Most controls default focusTraversable to true.
                    // Give a way to have them return the correct default value.
                    return node.getInitialFocusTraversable();
                }

            };
        private static final CssMetaData<Node,Number> OPACITY =
            new CssMetaData<>("-fx-opacity",
                SizeConverter.getInstance(), 1.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.opacity == null || !node.opacity.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.opacityProperty();
                }
            };
        private static final CssMetaData<Node,BlendMode> BLEND_MODE =
            new CssMetaData<>("-fx-blend-mode", new EnumConverter<>(BlendMode.class)) {

                @Override
                public boolean isSettable(Node node) {
                    return node.blendMode == null || !node.blendMode.isBound();
                }

                @Override
                public StyleableProperty<BlendMode> getStyleableProperty(Node node) {
                    return (StyleableProperty<BlendMode>)node.blendModeProperty();
                }
            };
        private static final CssMetaData<Node,Number> ROTATE =
            new CssMetaData<>("-fx-rotate",
                SizeConverter.getInstance(), 0.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.rotate == null
                        || node.nodeTransformation.canSetRotate();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.rotateProperty();
                }
            };
        private static final CssMetaData<Node,Number> SCALE_X =
            new CssMetaData<>("-fx-scale-x",
                SizeConverter.getInstance(), 1.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.scaleX == null
                        || node.nodeTransformation.canSetScaleX();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.scaleXProperty();
                }
            };
        private static final CssMetaData<Node,Number> SCALE_Y =
            new CssMetaData<>("-fx-scale-y",
                SizeConverter.getInstance(), 1.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.scaleY == null
                        || node.nodeTransformation.canSetScaleY();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.scaleYProperty();
                }
            };
        private static final CssMetaData<Node,Number> SCALE_Z =
            new CssMetaData<>("-fx-scale-z",
                SizeConverter.getInstance(), 1.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.scaleZ == null
                        || node.nodeTransformation.canSetScaleZ();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.scaleZProperty();
                }
            };
        private static final CssMetaData<Node,Number> TRANSLATE_X =
            new CssMetaData<>("-fx-translate-x",
                SizeConverter.getInstance(), 0.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.translateX == null
                        || node.nodeTransformation.canSetTranslateX();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.translateXProperty();
                }
            };
        private static final CssMetaData<Node,Number> TRANSLATE_Y =
            new CssMetaData<>("-fx-translate-y",
                SizeConverter.getInstance(), 0.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.translateY == null
                        || node.nodeTransformation.canSetTranslateY();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.translateYProperty();
                }
            };
        private static final CssMetaData<Node,Number> TRANSLATE_Z =
            new CssMetaData<>("-fx-translate-z",
                SizeConverter.getInstance(), 0.0) {

                @Override
                public boolean isSettable(Node node) {
                    return node.nodeTransformation == null
                        || node.nodeTransformation.translateZ == null
                        || node.nodeTransformation.canSetTranslateZ();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Node node) {
                    return (StyleableProperty<Number>)node.translateZProperty();
                }
            };
         private static final CssMetaData<Node, Number> VIEW_ORDER
                 = new CssMetaData<>("-fx-view-order",
                         SizeConverter.getInstance(), 0.0) {

                     @Override
                     public boolean isSettable(Node node) {
                         return node.miscProperties == null
                         || node.miscProperties.viewOrder == null
                         || !node.miscProperties.viewOrder.isBound();
                     }

                     @Override
                     public StyleableProperty<Number> getStyleableProperty(Node node) {
                         return (StyleableProperty<Number>) node.viewOrderProperty();
                     }
                 };
        private static final CssMetaData<Node,Boolean> VISIBILITY =
            new CssMetaData<>("visibility",
                new StyleConverter<String,Boolean>() {

                    @Override
                    // [ visible | hidden | collapse | inherit ]
                    public Boolean convert(ParsedValue<String, Boolean> value, Font font) {
                        final String sval = value != null ? value.getValue() : null;
                        return "visible".equalsIgnoreCase(sval);
                    }

                },
                Boolean.TRUE) {

                @Override
                public boolean isSettable(Node node) {
                    return node.visible == null || !node.visible.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Node node) {
                    return (StyleableProperty<Boolean>)node.visibleProperty();
                }
            };
        private static final CssMetaData<Node,Boolean> MANAGED =
            new CssMetaData<>("-fx-managed",
                    BooleanConverter.getInstance(), Boolean.TRUE) {

                @Override
                public boolean isSettable(Node node) {
                    return node.managed == null || !node.managed.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Node node) {
                    return (StyleableProperty<Boolean>)node.managedProperty();
                }
            };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

         static {

             final List<CssMetaData<? extends Styleable, ?>> styleables =
                     new ArrayList<>();
             styleables.add(CURSOR);
             styleables.add(EFFECT);
             styleables.add(FOCUS_TRAVERSABLE);
             styleables.add(OPACITY);
             styleables.add(BLEND_MODE);
             styleables.add(ROTATE);
             styleables.add(SCALE_X);
             styleables.add(SCALE_Y);
             styleables.add(SCALE_Z);
             styleables.add(VIEW_ORDER);
             styleables.add(TRANSLATE_X);
             styleables.add(TRANSLATE_Y);
             styleables.add(TRANSLATE_Z);
             styleables.add(VISIBILITY);
             styleables.add(MANAGED);
             STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        //
        // Super-lazy instantiation pattern from Bill Pugh. StyleableProperties
        // is referenced no earlier (and therefore loaded no earlier by the
        // class loader) than the moment that  getClassCssMetaData() is called.
        // This avoids loading the CssMetaData instances until the point at
        // which CSS needs the data.
        //
        return StyleableProperties.STYLEABLES;
    }

    /**
     * This method should delegate to {@link Node#getClassCssMetaData()} so that
     * a Node's CssMetaData can be accessed without the need for reflection.
     *
     * @return The CssMetaData associated with this node, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /*
     * @return  The Styles that match this CSS property for the given Node. The
     * list is sorted by descending specificity.
     */
    // SB-dependency: JDK-8092096 has been filed to track this
    static List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable) {
         return CssStyleHelper.getMatchingStyles(styleable, cssMetaData);
    }

    final ObservableMap<StyleableProperty<?>, List<Style>> getStyleMap() {
         ObservableMap<StyleableProperty<?>, List<Style>> map =
                 (ObservableMap<StyleableProperty<?>, List<Style>>)getProperties().get("STYLEMAP");
         Map<StyleableProperty<?>, List<Style>> ret = CssStyleHelper.getMatchingStyles(map, this);
         if (ret != null) {
             if (ret instanceof ObservableMap) return (ObservableMap)ret;
             return FXCollections.observableMap(ret);
         }
         return FXCollections.<StyleableProperty<?>, List<Style>>emptyObservableMap();
     }

     /*
      * JDK-8091202
      */
     // SB-dependency: JDK-8092096 has been filed to track this
     final void setStyleMap(ObservableMap<StyleableProperty<?>, List<Style>> styleMap) {
         if (styleMap != null) getProperties().put("STYLEMAP", styleMap);
         else getProperties().remove("STYLEMAP");
     }

    /*
     * Find CSS styles that were used to style this Node in its current pseudo-class state. The map will contain the styles from this node and,
     * if the node is a Parent, its children. The node corresponding to an entry in the Map can be obtained by casting a StyleableProperty key to a
     * javafx.beans.property.Property and calling getBean(). The List contains only those styles used to style the property and will contain
     * styles used to resolve lookup values.
     *
     * @param styleMap A Map to be populated with the styles. If null, a new Map will be allocated.
     * @return The Map populated with matching styles.
     */
    // SB-dependency: JDK-8092096 has been filed to track this
    Map<StyleableProperty<?>,List<Style>> findStyles(Map<StyleableProperty<?>,List<Style>> styleMap) {

        Map<StyleableProperty<?>, List<Style>> ret = CssStyleHelper.getMatchingStyles(styleMap, this);
        return (ret != null) ? ret : Collections.<StyleableProperty<?>, List<Style>>emptyMap();
    }

    /**
     * Flags used to indicate in which way this node is dirty (or whether it
     * is clean) and what must happen during the next CSS cycle on the
     * scenegraph.
     */
    CssFlags cssFlag = CssFlags.CLEAN;

    /**
     * Needed for testing.
     */
    final CssFlags getCSSFlags() { return cssFlag; }

    /**
     * Called when a CSS pseudo-class change would cause styles to be reapplied.
     */
    private void requestCssStateTransition() {
        // If there is no scene, then we cannot make it dirty, so we'll leave
        // the flag alone
        if (getScene() == null) return;
        // Don't bother doing anything if the cssFlag is not CLEAN.
        // If the flag indicates a DIRTY_BRANCH, the flag needs to be changed
        // to UPDATE to ensure that NodeHelper.processCSS is called on the node.
        if (cssFlag == CssFlags.CLEAN || cssFlag == CssFlags.DIRTY_BRANCH) {
            cssFlag = CssFlags.UPDATE;
            notifyParentsOfInvalidatedCSS();
        }
    }

    /**
     * Used to specify that a pseudo-class of this Node has changed. If the
     * pseudo-class is used in a CSS selector that matches this Node, CSS will
     * be reapplied. Typically, this method is called from the {@code invalidated}
     * method of a property that is used as a pseudo-class. For example:
     * <pre><code>
     *
     *     private static final PseudoClass MY_PSEUDO_CLASS_STATE = PseudoClass.getPseudoClass("my-state");
     *
     *     BooleanProperty myPseudoClassState = new BooleanPropertyBase(false) {
     *
     *           {@literal @}Override public void invalidated() {
     *                pseudoClassStateChanged(MY_PSEUDO_CLASS_STATE, get());
     *           }
     *
     *           {@literal @}Override public Object getBean() {
     *               return MyControl.this;
     *           }
     *
     *           {@literal @}Override public String getName() {
     *               return "myPseudoClassState";
     *           }
     *       };
     * </code></pre>
     * @param pseudoClass the pseudo-class that has changed state
     * @param active whether or not the state is active
     * @since JavaFX 8.0
     */
    public final void pseudoClassStateChanged(PseudoClass pseudoClass, boolean active) {

        final boolean modified = active
                ? pseudoClassStates.add(pseudoClass)
                : pseudoClassStates.remove(pseudoClass);

        if (modified && styleHelper != null) {
            final boolean isTransition = styleHelper.pseudoClassStateChanged(pseudoClass);
            if (isTransition) {
                requestCssStateTransition();
            }
        }
   }

    // package so that StyleHelper can get at it
    final ObservableSet<PseudoClass> pseudoClassStates = new PseudoClassState();
    private final ObservableSet<PseudoClass> unmodifiablePseudoClassStates =
            FXCollections.unmodifiableObservableSet(pseudoClassStates);
    /**
     * @return The active pseudo-class states of this Node, wrapped in an unmodifiable ObservableSet
     * @since JavaFX 8.0
     */
    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return unmodifiablePseudoClassStates;
    }

    // Walks up the tree telling each parent that the pseudo class state of
    // this node has changed.
    final void notifyParentsOfInvalidatedCSS() {
        SubScene subScene = getSubScene();
        Parent root = (subScene != null) ?
                subScene.getRoot() : getScene().getRoot();

        if (!root.isDirty(DirtyBits.NODE_CSS)) {
            // Ensure that Scene.root is marked as dirty. If the scene isn't
            // dirty, nothing will get repainted. This bit is cleared from
            // Scene in doCSSPass().
            NodeHelper.markDirty(root, DirtyBits.NODE_CSS);
            if (subScene != null) {
                // If the node is part of a subscene, then we must ensure that
                // the we not only mark subScene.root dirty, but continue and
                // call subScene.notifyParentsOfInvalidatedCSS() until
                // Scene.root gets marked dirty, via the recursive call:
                subScene.cssFlag = CssFlags.UPDATE;
                subScene.notifyParentsOfInvalidatedCSS();
            }
        }
        Parent _parent = getParent();
        while (_parent != null) {
            if (_parent.cssFlag == CssFlags.CLEAN) {
                _parent.cssFlag = CssFlags.DIRTY_BRANCH;
                _parent = _parent.getParent();
            } else {
                _parent = null;
            }
        }
    }

    final void recalculateRelativeSizeProperties(Font fontForRelativeSizes) {
        if (styleHelper != null) {
            styleHelper.recalculateRelativeSizeProperties(this, fontForRelativeSizes);
        }
    }

    final void reapplyCSS() {

        if (getScene() == null) return;

        if (cssFlag == CssFlags.REAPPLY) return;

        if (cssFlag == CssFlags.DIRTY_BRANCH) {
            // JDK-8193445 - don't reapply CSS from here
            // Defer CSS application to this Node by marking cssFlag as REAPPLY
            cssFlag = CssFlags.REAPPLY;
            return;
        }

        // JDK-8095580 - don't reapply CSS in the middle of an update
        if (cssFlag == CssFlags.UPDATE) {
            cssFlag = CssFlags.REAPPLY;
            notifyParentsOfInvalidatedCSS();
            return;
        }

        reapplyCss();

        //
        // One idiom employed by developers is to, during the layout pass,
        // add or remove nodes from the scene. For example, a ScrollPane
        // might add scroll bars to itself if it determines during layout
        // that it needs them, or a ListView might add cells to itself if
        // it determines that it needs to. In such situations we must
        // apply the CSS immediately and not add it to the scene's queue
        // for deferred action.
        //
        if (getParent() != null && getParent().isPerformingLayout()) {
            NodeHelper.processCSS(this);
        } else {
            notifyParentsOfInvalidatedCSS();
        }

    }

    //
    // This method "reapplies" CSS to this node and all of its children. Reapplying CSS
    // means that new style maps are calculated for the node. The process of reapplying
    // CSS may reset the CSS properties of a node to their initial state, but the _new_
    // styles are not applied as part of this process.
    //
    // There is no check of the CSS state of a child since reapply takes precedence
    // over other CSS states.
    //
    private void reapplyCss() {

        // Hang on to current styleHelper so we can know whether
        // createStyleHelper returned the same styleHelper
        final CssStyleHelper oldStyleHelper = styleHelper;

        // CSS state is "REAPPLY"
        cssFlag = CssFlags.REAPPLY;

        styleHelper = CssStyleHelper.createStyleHelper(this);

        // REAPPLY to my children, too.
        if (this instanceof Parent) {

            // minor optimization to avoid calling createStyleHelper on children
            // when we know there will not be any change in the style maps.
            final boolean visitChildren =
                    // If we don't have a styleHelper, then we should visit the children of this parent
                    // since there might be styles that depend on being a child of this parent.
                    // In other words, we have .a > .b { blah: blort; }, but no styles for ".a" itself.
                    styleHelper == null ||
                    // if the styleHelper changed, then we definitely need to visit the children
                    // since the new styles may have an effect on the children's styles calculated values.
                    (oldStyleHelper != styleHelper) ||
                    // If our parent is null, then we're the root of a scene or sub-scene, most likely,
                    // and we'll visit children because elsewhere the code depends on root.reapplyCSS()
                    // to force css to be reapplied (whether it needs to be or not).
                    (getParent() == null) ||
                    // If our parent's cssFlag is other than clean, then the parent may have just had
                    // CSS reapplied. If the parent just had CSS reapplied, then some of its styles
                    // may affect my children's styles.
                    (getParent().cssFlag != CssFlags.CLEAN);

            if (visitChildren) {

                List<Node> children = ((Parent) this).getChildren();
                for (int n = 0, nMax = children.size(); n < nMax; n++) {
                    Node child = children.get(n);
                    child.reapplyCss();
                }
            }

        } else if (this instanceof SubScene) {

            // SubScene root is a Parent, but reapplyCss is a private method in Node
            final Node subSceneRoot = ((SubScene)this).getRoot();
            if (subSceneRoot != null) {
                subSceneRoot.reapplyCss();
            }

        } else if (styleHelper == null) {
            //
            // If this is not a Parent and there is no styleHelper, then the CSS state is "CLEAN"
            // since there are no styles to apply or children to update.
            //
            cssFlag = CssFlags.CLEAN;
            return;
        }

        cssFlag = CssFlags.UPDATE;

    }

    void processCSS() {
        switch (cssFlag) {
            case CLEAN:
                break;
            case DIRTY_BRANCH:
            {
                Parent me = (Parent)this;
                // clear the flag first in case the flag is set to something
                // other than clean by downstream processing.
                me.cssFlag = CssFlags.CLEAN;
                List<Node> children = me.getChildren();
                for (int i=0, max=children.size(); i<max; i++) {
                    children.get(i).processCSS();
                }
                break;
            }
            case REAPPLY:
            case UPDATE:
            default:
                NodeHelper.processCSS(this);
        }
    }

    /**
     * If required, apply styles to this Node and its children, if any. This method does not normally need to
     * be invoked directly but may be used in conjunction with {@link Parent#layout()} to size a Node before the
     * next pulse, or if the {@link #getScene() Scene} is not in a {@link javafx.stage.Stage}.
     * <p>Provided that the Node&#39;s {@link #getScene() Scene} is not null, CSS is applied to this Node regardless
     * of whether this Node&#39;s CSS state is clean. CSS styles are applied from the top-most parent
     * of this Node whose CSS state is other than clean, which may affect the styling of other nodes.
     * This method is a no-op if the Node is not in a Scene. The Scene does not have to be in a Stage.</p>
     * <p>This method does not invoke the {@link Parent#layout()} method. Typically, the caller will use the
     * following sequence of operations.</p>
     * <pre>{@code
     *     parentNode.applyCss();
     *     parentNode.layout();
     * }</pre>
     * <p>As a more complete example, the following code uses {@code applyCss()} and {@code layout()} to find
     * the width and height of the Button before the Stage has been shown. If either the call to {@code applyCss()}
     * or the call to {@code layout()} is commented out, the calls to {@code getWidth()} and {@code getHeight()}
     * will return zero (until some time after the Stage is shown). </p>
     * <pre><code>
     * {@literal @}Override
     * public void start(Stage stage) throws Exception {
     *
     *    Group root = new Group();
     *    Scene scene = new Scene(root);
     *
     *    Button button = new Button("Hello World");
     *    root.getChildren().add(button);
     *
     *    root.applyCss();
     *    root.layout();
     *
     *    double width = button.getWidth();
     *    double height = button.getHeight();
     *
     *    System.out.println(width + ", " + height);
     *
     *    stage.setScene(scene);
     *    stage.show();
     * }
     * </code></pre>
     * @since JavaFX 8.0
     */
    public final void applyCss() {

        if (getScene() == null) {
            return;
        }

        // update, unless reapply
        if (cssFlag != CssFlags.REAPPLY) cssFlag = CssFlags.UPDATE;

        //
        // JDK-8115093 - need to see if any ancestor has a flag UPDATE
        // If so, process css from the top-most CssFlags.UPDATE node
        // since my ancestor's styles may affect mine.
        //
        // If the scene-graph root isn't NODE_CSS dirty, then all my
        // ancestor flags should be CLEAN and I can skip this lookup.
        //
        Node topMost = this;

        final boolean dirtyRoot = getScene().getRoot().isDirty(com.sun.javafx.scene.DirtyBits.NODE_CSS);
        if (dirtyRoot) {

            Node _parent = getParent();
            while (_parent != null) {
                if (_parent.cssFlag == CssFlags.UPDATE || _parent.cssFlag == CssFlags.REAPPLY) {
                    topMost = _parent;
                }
                _parent = _parent.getParent();
            }

            // Note: this code used to mark the parent nodes with DIRTY_BRANCH,
            // but that isn't necessary since UPDATE will apply css to all of
            // a Parent's children.

            // If we're at the root of the scene-graph, make sure the NODE_CSS
            // dirty bit is cleared (see Scene#doCSSPass())
            if (topMost == getScene().getRoot()) {
                getScene().getRoot().clearDirty(DirtyBits.NODE_CSS);
            }
        }

        topMost.processCSS();

    }

    /*
     * If invoked, will update styles from here on down. This method should not be called directly. If
     * overridden, the overriding method must at some point call {@code super.processCSSImpl} to ensure that
     * this Node's CSS state is properly updated.
     *
     * Note that the difference between this method and {@link #applyCss()} is that this method
     * updates styles for this node on down; whereas, {@code applyCss()} looks for the top-most ancestor that needs
     * CSS update and apply styles from that node on down.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doProcessCSS() {

        // Nothing to do...
        if (cssFlag == CssFlags.CLEAN) return;

        // if REAPPLY was deferred, process it now...
        if (cssFlag == CssFlags.REAPPLY) {
            reapplyCss();
        }

        // Clear the flag first in case the flag is set to something
        // other than clean by downstream processing.
        cssFlag = CssFlags.CLEAN;

        // Transition to the new state and apply styles
        if (styleHelper != null && getScene() != null) {
            styleHelper.transitionToState(this);
        }
    }

    /**
     * A node is considered to be in its initial CSS state if it wasn't shown in a scene graph before.
     * This flag is cleared after CSS processing was completed in a Scene pulse event. Note that manual
     * calls to {@link #applyCss()} or similar methods will not clear this flag, since we consider all
     * CSS processing before the Scene pulse to be part of the node's initial state.
     */
    private boolean initialCssState = true;

    private void resetInitialCssStateFlag() {
        initialCssState = true;
        Scene scene = getScene();
        if (scene != null) {
            scene.registerClearInitialCssStateFlag(this);
        }
    }

    void clearInitialCssStateFlag() {
        initialCssState = false;
    }

    private MediaQueryContext getMediaQueryContext() {
        Scene scene = getScene();
        return scene != null ? scene.preferences : null;
    }

    /**
     * A StyleHelper for this node.
     * A StyleHelper contains all the css styles for this node
     * and knows how to apply them when our state changes.
     */
    CssStyleHelper styleHelper;

    private static final PseudoClass HOVER_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("hover");
    private static final PseudoClass PRESSED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("pressed");
    private static final PseudoClass DISABLED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("disabled");
    private static final PseudoClass FOCUSED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("focused");
    private static final PseudoClass FOCUS_VISIBLE_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("focus-visible");
    private static final PseudoClass FOCUS_WITHIN_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("focus-within");
    private static final PseudoClass SHOW_MNEMONICS_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("show-mnemonics");

    private static abstract class LazyTransformProperty
            extends ReadOnlyObjectPropertyBase<Transform> {

        protected static final int VALID = 0;
        protected static final int INVALID = 1;
        protected static final int VALIDITY_UNKNOWN = 2;
        protected int valid = INVALID;

        private Transform transform;
        private boolean canReuse = false;

        protected Transform getInternalValue() {
            if (valid == INVALID ||
                    (valid == VALIDITY_UNKNOWN && computeValidity() == INVALID)) {
                transform = computeTransform(canReuse ? transform : null);
                canReuse = true;
                valid = validityKnown() ? VALID : VALIDITY_UNKNOWN;
            }

            return transform;
        }

        @Override
        public Transform get() {
            transform = getInternalValue();
            canReuse = false;
            return transform;
        }

        public void validityUnknown() {
            if (valid == VALID) {
                valid = VALIDITY_UNKNOWN;
            }
        }

        public void invalidate() {
            if (valid != INVALID) {
                valid = INVALID;
                fireValueChangedEvent();
            }
        }

        protected abstract boolean validityKnown();
        protected abstract int computeValidity();
        protected abstract Transform computeTransform(Transform reuse);
    }

    private static abstract class LazyBoundsProperty
            extends ReadOnlyObjectPropertyBase<Bounds> {
        private boolean valid;

        private Bounds bounds;

        @Override
        public Bounds get() {
            if (!valid) {
                bounds = computeBounds();
                valid = true;
            }

            return bounds;
        }

        public void invalidate() {
            if (valid) {
                valid = false;
                fireValueChangedEvent();
            }
        }

        protected abstract Bounds computeBounds();
    }

    private static final BoundsAccessor boundsAccessor = (bounds, tx, node) -> node.getGeomBounds(bounds, tx);

    /**
     * The accessible role for this {@code Node}.
     * <p>
     * The screen reader uses the role of a node to determine the
     * attributes and actions that are supported.
     *
     * @defaultValue {@link AccessibleRole#NODE}
     * @see AccessibleRole
     *
     * @since JavaFX 8u40
     */
    private ObjectProperty<AccessibleRole> accessibleRole;

    public final void setAccessibleRole(AccessibleRole value) {
        if (value == null) value = AccessibleRole.NODE;
        accessibleRoleProperty().set(value);
    }

    public final AccessibleRole getAccessibleRole() {
        if (accessibleRole == null) return AccessibleRole.NODE;
        return accessibleRoleProperty().get();
    }

    public final ObjectProperty<AccessibleRole> accessibleRoleProperty() {
        if (accessibleRole == null) {
            accessibleRole = new SimpleObjectProperty<>(this, "accessibleRole", AccessibleRole.NODE);
        }
        return accessibleRole;
    }

    public final void setAccessibleRoleDescription(String value) {
        accessibleRoleDescriptionProperty().set(value);
    }

    public final String getAccessibleRoleDescription() {
        if (accessibilityProperties == null) return null;
        if (accessibilityProperties.accessibleRoleDescription == null) return null;
        return accessibleRoleDescriptionProperty().get();
    }

    /**
     * The role description of this {@code Node}.
     * <p>
     * Normally, when a role is provided for a node, the screen reader
     * speaks the role as well as the contents of the node.  When this
     * value is set, it is possible to override the default.  This is
     * useful because the set of roles is predefined.  For example,
     * it is possible to set the role of a node to be a button, but
     * have the role description be arbitrary text.
     *
     * @return the role description of this {@code Node}.
     * @defaultValue null
     *
     * @since JavaFX 8u40
     */
    public final ObjectProperty<String> accessibleRoleDescriptionProperty() {
        return getAccessibilityProperties().getAccessibleRoleDescription();
    }

    public final void setAccessibleText(String value) {
        accessibleTextProperty().set(value);
    }

    public final String getAccessibleText() {
        if (accessibilityProperties == null) return null;
        if (accessibilityProperties.accessibleText == null) return null;
        return accessibleTextProperty().get();
    }

    /**
     * The accessible text for this {@code Node}.
     * <p>
     * This property is used to set the text that the screen
     * reader will speak.  If a node normally speaks text,
     * that text is overriden.  For example, a button
     * usually speaks using the text in the control but will
     * no longer do this when this value is set.
     *
     * @return accessible text for this {@code Node}.
     * @defaultValue null
     *
     * @since JavaFX 8u40
     */
    public final ObjectProperty<String> accessibleTextProperty() {
        return getAccessibilityProperties().getAccessibleText();
    }

    public final void setAccessibleHelp(String value) {
        accessibleHelpProperty().set(value);
    }

    public final String getAccessibleHelp() {
        if (accessibilityProperties == null) return null;
        if (accessibilityProperties.accessibleHelp == null) return null;
        return accessibleHelpProperty().get();
    }

    /**
     * The accessible help text for this {@code Node}.
     * <p>
     * The help text provides a more detailed description of the
     * accessible text for a node.  By default, if the node has
     * a tool tip, this text is used.
     *
     * @return the accessible help text for this {@code Node}.
     * @defaultValue null
     *
     * @since JavaFX 8u40
     */
    public final ObjectProperty<String> accessibleHelpProperty() {
        return getAccessibilityProperties().getAccessibleHelp();
    }

    AccessibilityProperties accessibilityProperties;
    private AccessibilityProperties getAccessibilityProperties() {
        if (accessibilityProperties == null) {
            accessibilityProperties = new AccessibilityProperties();
        }
        return accessibilityProperties;
    }

    private class AccessibilityProperties {
        ObjectProperty<String> accessibleRoleDescription;
        ObjectProperty<String> getAccessibleRoleDescription() {
            if (accessibleRoleDescription == null) {
                accessibleRoleDescription = new SimpleObjectProperty<>(Node.this, "accessibleRoleDescription", null);
            }
            return accessibleRoleDescription;
        }
        ObjectProperty<String> accessibleText;
        ObjectProperty<String> getAccessibleText() {
            if (accessibleText == null) {
                accessibleText = new SimpleObjectProperty<>(Node.this, "accessibleText", null);
            }
            return accessibleText;
        }
        ObjectProperty<String> accessibleHelp;
        ObjectProperty<String> getAccessibleHelp() {
            if (accessibleHelp == null) {
                accessibleHelp = new SimpleObjectProperty<>(Node.this, "accessibleHelp", null);
            }
            return accessibleHelp;
        }
    }

    /**
     * This method is called by the assistive technology to request
     * the value for an attribute.
     * <p>
     * This method is commonly overridden by subclasses to implement
     * attributes that are required for a specific role.<br>
     * If a particular attribute is not handled, the superclass implementation
     * must be called.
     * </p>
     *
     * @param attribute the requested attribute
     * @param parameters optional list of parameters
     * @return the value for the requested attribute
     *
     * @see AccessibleAttribute
     *
     * @since JavaFX 8u40
     */
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case ROLE: return getAccessibleRole();
            case ROLE_DESCRIPTION: return getAccessibleRoleDescription();
            case TEXT: return getAccessibleText();
            case HELP: return getAccessibleHelp();
            case PARENT: return getParent();
            case SCENE: return getScene();
            case BOUNDS: return localToScreen(getBoundsInLocal());
            case DISABLED: return isDisabled();
            case FOCUSED: return isFocused();
            case VISIBLE: return isVisible();
            case LABELED_BY: return labeledBy;
            default: return null;
        }
    }

    /**
     * This method is called by the assistive technology to request the action
     * indicated by the argument should be executed.
     * <p>
     * This method is commonly overridden by subclasses to implement
     * action that are required for a specific role.<br>
     * If a particular action is not handled, the superclass implementation
     * must be called.
     * </p>
     *
     * @param action the action to execute
     * @param parameters optional list of parameters
     *
     * @see AccessibleAction
     *
     * @since JavaFX 8u40
     */
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case REQUEST_FOCUS:
                if (isFocusTraversable()) {
                    requestFocus();
                }
                break;
            case SHOW_MENU: {
                Bounds b = getBoundsInLocal();
                Point2D pt = localToScreen(b.getMaxX(), b.getMaxY());
                ContextMenuEvent event =
                    new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    b.getMaxX(), b.getMaxY(), pt.getX(), pt.getY(),
                    false, new PickResult(this, b.getMaxX(), b.getMaxY()));
                Event.fireEvent(this, event);
                break;
            }
            default:
        }
    }

    /**
     * This method is called by the application to notify the assistive
     * technology that the value for an attribute has changed.
     *
     * @param attributes the attribute whose value has changed
     *
     * @see AccessibleAttribute
     *
     * @since JavaFX 8u40
     */
    public final void notifyAccessibleAttributeChanged(AccessibleAttribute attributes) {
        if (accessible == null) {
            Scene scene = getScene();
            if (scene != null) {
                accessible = scene.removeAccessible(this);
            }
        }
        if (accessible != null) {
            accessible.sendNotification(attributes);
        }
    }

    Accessible accessible;
    Accessible getAccessible() {
        if (accessible == null) {
            Scene scene = getScene();
            /* It is possible the node was reparented and getAccessible()
             * is called before the pulse. Try to recycle the accessible
             * before creating a new one.
             * Note: this code relies that an accessible can never be on
             * more than one Scene#accMap. Thus, the only way
             * scene#removeAccessible() returns non-null is if the node
             * old scene and new scene are the same object.
             */
            if (scene != null) {
                accessible = scene.removeAccessible(this);
            }
        }
        if (accessible == null) {
            accessible = Application.GetApplication().createAccessible();
            accessible.setEventHandler(new Accessible.EventHandler() {
                @Override public Object getAttribute(AccessibleAttribute attribute, Object... parameters) {
                    return queryAccessibleAttribute(attribute, parameters);
                }
                @Override public void executeAction(AccessibleAction action, Object... parameters) {
                    executeAccessibleAction(action, parameters);
                }
                @Override public String toString() {
                    String klassName = Node.this.getClass().getName();
                    return klassName.substring(klassName.lastIndexOf('.')+1);
                }
            });
        }
        return accessible;
    }

    void releaseAccessible() {
        Accessible acc = this.accessible;
        if (acc != null) {
            accessible = null;
            acc.dispose();
        }
    }

}
