/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>Provides the core set of base
 * classes for the JavaFX Scene Graph API. A scene graph is a tree-like
 * data structure, where each item in the tree has zero or one parent and
 * zero or more children.</p>
 *
 * <p>The two primary classes in this package are:</p>
 *
 * <ul>
 *
 * <li>{@link javafx.scene.Scene Scene} &ndash; Defines the scene to be rendered. It
 * contains a {@code fill} variable that specifies the background of
 * the scene, {@code width} and {@code height} variables that
 * specify the size of the scene, and a {@code content} sequence
 * that contains a list of "root" {@code Nodes} to be rendered onto
 * the scene. This sequence of {@code Nodes} is the scene graph for
 * this {@code Scene}.
 * A {@code Scene} is rendered onto a {@link javafx.stage.Stage}, which is the
 * top-level container for JavaFX content.</li>
 *
 * <li>{@link javafx.scene.Node Node} &ndash; Abstract base class for all nodes in the
 * scene graph. Each node is either a "leaf" node with no child nodes or
 * a "branch" node with zero or more child nodes. Each node in the tree
 * has zero or one parent. Only a single node within each tree in the
 * scene graph will have no parent, which is often referred to as the
 * "root" node.
 * There may be several trees in the scene graph. Some trees may be part of
 * a {@link javafx.scene.Scene Scene}, in which case they are eligible to be displayed.
 * Other trees might not be part of any {@link javafx.scene.Scene Scene}.</li>
 *
 * </ul>
 *
 * <p>Branch nodes are of type {@link javafx.scene.Parent Parent} or
 * subclasses thereof.</p>
 *
 * <p>Leaf nodes are classes such as
 * {@link javafx.scene.shape.Rectangle}, {@link javafx.scene.text.Text},
 * {@link javafx.scene.image.ImageView}, {@link javafx.scene.media.MediaView},
 * or other such leaf classes which cannot have children.
 *
 * <p>A node may occur at most once anywhere in the scene
 * graph. Specifically, a node must appear no more than once in the children
 * list of a {@link javafx.scene.Parent Parent} or as the clip of a
 * {@link javafx.scene.Node Node}.
 * See the {@link javafx.scene.Node Node} class for more details on these restrictions.</p>
 *
 * <h2>Example</h2>
 *
 * <p>An example JavaFX scene graph is as follows:</p>
 *
 * <pre>
 * package example;
 *
 * import javafx.application.Application;
 * import javafx.stage.Stage;
 * import javafx.scene.Scene;
 * import javafx.scene.Group;
 * import javafx.scene.paint.Color;
 * import javafx.scene.shape.Circle;
 * import javafx.scene.text.Text;
 * import javafx.scene.text.Font;
 *
 * public class Example extends Application {
 *
 *     &#64;Override public void start(Stage stage) {
 *
 *         Group root = new Group();
 *         Scene scene = new Scene(root, 200, 150);
 *         scene.setFill(Color.LIGHTGRAY);
 *
 *         Circle circle = new Circle(60, 40, 30, Color.GREEN);
 *
 *         Text text = new Text(10, 90, "JavaFX Scene");
 *         text.setFill(Color.DARKRED);
 *
 *         Font font = new Font(20);
 *         text.setFont(font);
 *
 *         root.getChildren().add(circle);
 *         root.getChildren().add(text);
 *         stage.setScene(scene);
 *         stage.show();
 *     }
 *
 *     public static void main(String[] args) {
 *         Application.launch(args);
 *     }
 * }
 * </pre>
 *
 * <p>The above example will generate the following image:</p>
 *
 * <p><img src="doc-files/Scene1.png" alt="A visual rendering of the JavaFX Scene example"></p>
 *
 * <h2>Coordinate System and Transformations</h2>
 *
 * <p>The {@code Node} class defines a traditional computer graphics "local"
 * coordinate system in which the {@code x} axis increases to the right and the
 * {@code y} axis increases downwards. The concrete node classes for shapes
 * provide variables for defining the geometry and location of the shape
 * within this local coordinate space. For example,
 * {@link javafx.scene.shape.Rectangle} provides {@code x}, {@code y},
 * {@code width}, {@code height} variables while
 * {@link javafx.scene.shape.Circle} provides {@code centerX}, {@code centerY},
 * and {@code radius}.</p>
 *
 * <p>Any {@code Node} can have transformations applied to it. These include
 * translation, rotation, scaling, or shearing transformations. A transformation
 * will change the position, orientation, or size of the coordinate system as
 * viewed from the parent of the node that has been transformed.</p>
 *
 * <p>See the {@link javafx.scene.Node Node} class for more information on transformations.</p>
 *
 * <h2>Bounding Rectangle</h2>
 *
 * <p>Since every {@code Node} has transformations, every Node's geometric
 * bounding rectangle can be described differently depending on whether
 * transformations are accounted for or not.</p>
 *
 * <p>Each {@code Node} has the following properties which
 * specifies these bounding rectangles:</p>
 *
 * <ul>
 *
 * <li>{@code boundsInLocal} &ndash; specifies the bounds of the
 * {@code Node} in untransformed local coordinates.</li>
 *
 * <li>{@code boundsInParent} &ndash; specifies the bounds of the
 * {@code Node} after all transformations have been applied.
 * It is called "boundsInParent" because the
 * rectangle will be relative to the parent's coordinate system.</li>
 *
 * <li>{@code layoutBounds} &ndash; specifies the rectangular bounds of
 * the {@code Node} that should be used as the basis for layout
 * calculations, and may differ from the visual bounds of the node. For
 * shapes, Text, and ImageView, the default {@code layoutBounds} includes
 * only the shape geometry.</li>
 *
 * </ul>
 *
 * <p>See the {@link javafx.scene.Node Node} class for more information on bounding rectangles.</p>
 *
 * <h2>CSS</h2>
 * <p>
 * The JavaFX Scene Graph provides the facility to style nodes using
 * CSS (Cascading Style Sheets).
 * The {@link javafx.scene.Node Node} class contains {@code id}, {@code styleClass}, and
 * {@code style} variables are used by CSS selectors to find nodes
 * to which styles should be applied. The {@link javafx.scene.Scene Scene} class contains
 * the {@code stylesheets} variable which is a sequence of URLs that
 * reference CSS style sheets that are to be applied to the nodes within
 * that scene.
 * <p>
 * For further information about CSS, how to apply CSS styles
 * to nodes, and what properties are available for styling, see the
 * <a href="doc-files/cssref.html">CSS Reference Guide</a>.
 */
package javafx.scene;
